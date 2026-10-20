#!/usr/bin/env node
/**
 * redact-live-values.mjs - move every live credential out of the tracked files.
 *
 * The reverse engineering documents quote the original's behaviour, and quoting behaviour
 * sometimes means quoting a credential. Those quotes are exactly what must not reach a
 * public repository.
 *
 * This tool takes the live values straight from the original artifact - the shipped
 * `application.properties` and the seeded SQL - and replaces every occurrence in the
 * tracked files with a marker. The values themselves stay in `SECRETS.txt`, which is
 * git-ignored, so the owner can still test which of them still work.
 *
 * It is idempotent: running it twice changes nothing the second time.
 *
 * Usage: node tools/redact-live-values.mjs [--check]
 *   --check  report what would be redacted and exit non-zero if anything is left
 */
import { readFileSync, writeFileSync, existsSync, readdirSync, statSync } from "node:fs";
import { join, dirname, extname, relative } from "node:path";
import { fileURLToPath } from "node:url";
import { execFileSync } from "node:child_process";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..");
const ORIGINAL_DIR = "/Users/saiduttaabhishekdash/seo-0.0.1-SNAPSHOT";
const ORIGINAL_PROPERTIES = "/tmp/seo-re/work/extracted/BOOT-INF/classes/application.properties";
const ORIGINAL_SQL = join(ORIGINAL_DIR, "emailsend21.sql");
const CHECK_ONLY = process.argv.includes("--check");

const MARKER = "[redacted - see SECRETS.txt]";

/** The text files that are tracked and may quote a live value. */
const TEXT_EXT = new Set([".md", ".java", ".html", ".yml", ".yaml", ".properties", ".sql", ".js", ".mjs", ".json", ".xml", ".txt", ".sh", ".css"]);
const SKIP_DIRS = new Set([".git", "target", "node_modules", ".unlazy"]);
/** Never redact these: they are the record of the leak, and they are git-ignored. */
const SKIP_FILES = new Set(["SECRETS.txt", "redact-live-values.mjs"]);

/**
 * Collects the live values from the original artifact.
 *
 * Everything is read from the artifact itself, so a value that was missed by hand is
 * still caught here.
 */
function collectLiveValues() {
  const values = new Map(); // value -> what it is

  const add = (value, what) => {
    if (!value || typeof value !== "string") return;
    const v = value.trim();
    if (v.length < 8) return; // too short to be a meaningful credential
    if (!values.has(v)) values.set(v, what);
  };

  // ---- the shipped configuration ----
  if (existsSync(ORIGINAL_PROPERTIES)) {
    for (const line of readFileSync(ORIGINAL_PROPERTIES, "utf8").split(/\r?\n/)) {
      const t = line.trim();
      if (!t || t.startsWith("#")) continue;
      const i = t.indexOf("=");
      if (i < 0) continue;
      const key = t.slice(0, i).trim();
      const value = t.slice(i + 1).trim();
      if (!value || value.startsWith("$")) continue;
      if (/password|license|mac|token|secret/i.test(key)) add(value, `config:${key}`);
    }
  }

  // ---- the seeded database ----
  if (existsSync(ORIGINAL_SQL)) {
    const sql = readFileSync(ORIGINAL_SQL, "utf8");

    // Google OAuth material and every credential column in a seeded row.
    for (const table of ["gmail_master", "email_master", "login"]) {
      const m = new RegExp(
        `insert\\s+into\\s+\`${table}\`\\s*\\([^)]*\\)\\s*values\\s*([\\s\\S]*?);\\s*\\n`,
        "i"
      ).exec(sql);
      if (!m) continue;
      for (const row of m[1].split(/\),\s*\n/g)) {
        for (const cell of row.split(/,\s*(?=(?:[^']*'[^']*')*[^']*$)/)) {
          const value = cell.trim().replace(/^'|'$/g, "");
          if (!value || value === "NULL") continue;
          if (/^(1\/\/|ya29\.|GOCSPX-|\$2[aby]\$|[\w.+-]+@[\w.-]+$|[A-Za-z0-9-]{20,}\.apps\.googleusercontent\.com$)/.test(value)) {
            add(value, `db:${table}`);
          }
        }
      }
    }
  }

  // The personal data that identifies the accounts behind the credentials comes out of
  // the same seeded rows, so this tool holds no live value of its own.
  return values;
}

/** Every tracked text file. */
function trackedTextFiles() {
  const out = [];
  const walk = (dir) => {
    for (const e of readdirSync(dir)) {
      if (SKIP_DIRS.has(e)) continue;
      const p = join(dir, e);
      let st;
      try {
        st = statSync(p);
      } catch {
        continue;
      }
      if (st.isDirectory()) walk(p);
      else if (TEXT_EXT.has(extname(p)) && !SKIP_FILES.has(basename(p))) out.push(p);
    }
  };
  walk(ROOT);
  return out;
}

const basename = (p) => p.split("/").pop();

const liveValues = collectLiveValues();
console.log(`[redact] ${liveValues.size} live value(s) taken from the original artifact`);

const findings = [];
let changedFiles = 0;

for (const file of trackedTextFiles()) {
  const src = readFileSync(file, "utf8");
  let out = src;
  for (const [value, what] of liveValues) {
    if (!out.includes(value)) continue;
    findings.push({ file: relative(ROOT, file), what, sample: value.slice(0, 6) + "..." });
    out = out.split(value).join(MARKER);
  }
  if (out !== src) {
    changedFiles++;
    if (!CHECK_ONLY) writeFileSync(file, out, "utf8");
  }
}

if (findings.length) {
  console.log(`[redact] ${findings.length} live value occurrence(s) in ${changedFiles} file(s):`);
  for (const f of findings) console.log(`  ${f.file}  [${f.what}]  ${f.sample}`);
} else {
  console.log("[redact] no live value is present in any tracked file");
}

if (CHECK_ONLY) {
  if (findings.length) {
    console.error("REDACT-FAIL: live values are still present in tracked files");
    process.exit(1);
  }
  console.log("REDACT-PASS: no live value is present in any tracked file");
  process.exit(0);
}

console.log(`[redact] ${changedFiles} file(s) updated; the values remain in SECRETS.txt`);
console.log("REDACT-PASS");
