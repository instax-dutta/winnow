#!/usr/bin/env node
/**
 * no-secrets.mjs - fail if a tracked source file contains a credential-shaped literal.
 *
 * Scans git-tracked text files (plus the untracked-but-present source tree) for live
 * credential patterns. The rules mirror the root AGENTS.md "No secret in source" rule.
 *
 * Usage: node tools/verify/no-secrets.mjs
 * Exit 0 and print "SECRET-SCAN-PASS" when clean, exit 1 with findings otherwise.
 */
import { readFileSync, readdirSync, statSync, existsSync } from "node:fs";
import { join, relative, extname } from "node:path";
import { fileURLToPath } from "node:url";
import { execFileSync } from "node:child_process";

const ROOT = join(fileURLToPath(import.meta.url), "..", "..", "..");
const rel = (p) => relative(ROOT, p);

const SKIP_DIRS = new Set([".git", "target", "node_modules", "reference", ".unlazy", "docs"]);
const TEXT_EXT = new Set([
  ".java", ".xml", ".yml", ".yaml", ".properties", ".sql", ".html", ".js", ".mjs",
  ".json", ".md", ".txt", ".css", ".sh", ".ts",
]);

// Files allowed to contain credential-shaped text (the harvest report itself, the
// analysis of the original, and local-only configuration that is git-ignored).
const ALLOW = new Set(["SECRETS.txt"]);

const RULES = [
  { id: "google-oauth-client-secret", re: /\bGOCSPX-[A-Za-z0-9_-]{20,}/g },
  { id: "google-oauth-client-id", re: /\b\d{6,}-[0-9a-z]{20,}\.apps\.googleusercontent\.com/g },
  { id: "google-oauth-refresh-token", re: /\b1\/\/[0-9A-Za-z_-]{40,}/g },
  { id: "google-api-key", re: /\bAIza[0-9A-Za-z_-]{35}\b/g },
  { id: "aws-access-key", re: /\bAKIA[0-9A-Z]{16}\b/g },
  { id: "stripe-key", re: /\b(?:sk|pk)_(?:live|test)_[0-9A-Za-z]{16,}\b/g },
  { id: "slack-token", re: /\bxox[baprs]-[0-9A-Za-z-]{10,}/g },
  { id: "private-key-block", re: /-----BEGIN [A-Z ]*PRIVATE KEY-----/g },
  { id: "jwt-literal", re: /\beyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}/g },
];

// Assignment of a credential-looking name to a non-placeholder literal.
const ASSIGN =
  /(?:password|passwd|pwd|secret|api[_-]?key|apikey|client[_-]?secret|refresh[_-]?token|access[_-]?token|wordpress\.api\.token)\s*[:=]\s*["']([^"'\s]{8,})["']/gi;

// A value that is not a secret: a placeholder, a variable reference, or the documented
// default password of a fresh install, which is published in the README on purpose.
const PLACEHOLDER = new RegExp(
  "^(?:" +
    "\\$\\{[^}]+\\}" +        // ${VAR}
    "|\\$[A-Za-z_][A-Za-z0-9_]*" + // $VAR
    "|<[^>]+>" +                   // <placeholder>
    "|change[-_ ]?me[-_ ]?now" +   // the published default of a fresh install
    "|changeme" +
    "|placeholder" +
    "|example" +
    "|redacted" +
    "|xxx+" +
    "|\\*{3,}" +
    "|dummy" +
    "|test[-_ ]?value" +
    "|your[-_ ].*" +
    "|null" +
    "|none" +
    ")$",
  "i"
);

const walk = (dir) => {
  const out = [];
  for (const e of readdirSync(dir)) {
    if (SKIP_DIRS.has(e)) continue;
    const p = join(dir, e);
    const st = statSync(p);
    if (st.isDirectory()) out.push(...walk(p));
    else out.push(p);
  }
  return out;
};

const files = walk(ROOT).filter((f) => TEXT_EXT.has(extname(f)) && !ALLOW.has(rel(f)));

const findings = [];
for (const f of files) {
  let src;
  try {
    src = readFileSync(f, "utf8");
  } catch {
    continue;
  }
  for (const rule of RULES) {
    for (const m of src.matchAll(rule.re)) {
      findings.push({ file: rel(f), rule: rule.id, match: m[0].slice(0, 24) + "..." });
    }
  }
  for (const m of src.matchAll(ASSIGN)) {
    const value = m[1];
    if (PLACEHOLDER.test(value)) continue;
    findings.push({ file: rel(f), rule: "credential-assignment", match: `${m[0].slice(0, 40)}...` });
  }
}

if (findings.length) {
  console.error("SECRET-SCAN-FAIL: credential-shaped literals found in tracked source");
  for (const f of findings) console.error(`  ${f.file}  [${f.rule}]  ${f.match}`);
  process.exit(1);
}
console.log(`SECRET-SCAN-PASS: ${files.length} files scanned, 0 credential literals`);
