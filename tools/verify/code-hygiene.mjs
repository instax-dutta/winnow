#!/usr/bin/env node
/**
 * code-hygiene.mjs - enforce the hard rules from the root AGENTS.md against the source tree.
 *
 * Usage: node tools/verify/code-hygiene.mjs
 * Exit 0 and print "HYGIENE-PASS" when clean, exit 1 with findings otherwise.
 */
import { readFileSync, readdirSync, statSync } from "node:fs";
import { join, relative, extname } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = join(fileURLToPath(import.meta.url), "..", "..", "..");
const rel = (p) => relative(ROOT, p);

const SRC = join(ROOT, "src");
const SKIP_DIRS = new Set(["target", "node_modules"]);

const walk = (dir, keep) => {
  const out = [];
  if (!existsOk(dir)) return out;
  for (const e of readdirSync(dir)) {
    if (SKIP_DIRS.has(e)) continue;
    const p = join(dir, e);
    const st = statSync(p);
    if (st.isDirectory()) out.push(...walk(p, keep));
    else if (keep(p)) out.push(p);
  }
  return out;
};
const existsOk = (d) => {
  try {
    statSync(d);
    return true;
  } catch {
    return false;
  }
};

const javaFiles = walk(SRC, (p) => extname(p) === ".java");
const findings = [];
const add = (file, rule, line, text) => findings.push({ file: rel(file), rule, line, text: text.trim().slice(0, 100) });

for (const f of javaFiles) {
  const lines = readFileSync(f, "utf8").split(/\r?\n/);

  lines.forEach((line, i) => {
    const n = i + 1;

    // Rule 1: no printStackTrace
    if (/\.printStackTrace\s*\(/.test(line)) add(f, "printStackTrace", n, line);

    // Rule 2: no System.out / System.err in application code
    if (/System\.(out|err)\s*\./.test(line)) add(f, "System.out.println", n, line);

    // Rule 3: no shared SimpleDateFormat as an instance field
    if (/^\s*(private|public|protected)\s+(static\s+)?(final\s+)?SimpleDateFormat\s+\w+\s*=/.test(line))
      add(f, "SimpleDateFormat-field", n, line);

    // Rule 4: no java.sql.Date / java.util.Date in entity or service code
    if (/\bjava\.sql\.Date\b/.test(line)) add(f, "java.sql.Date", n, line);

    // Rule 5: no @GetMapping that mutates
    if (/@GetMapping/.test(line) && /(remove|delete|save|create|update|approve|send|extract|import|upload)/i.test(line))
      add(f, "state-change-on-GET", n, line);

    // Rule 6: no Hibernate @Where class filter (silently bypassed by native queries)
    if (/@Where\s*\(/.test(line)) add(f, "hibernate-@Where", n, line);

    // Rule 7: no hibernate.ddl-auto outside the test profile
    if (/ddl-auto\s*:\s*(?!none|validate)/.test(line) && !f.includes("test"))
      add(f, "ddl-auto-not-none", n, line);

    // Rule 8: no findAll() on a repository (tenant-owned tables are never dumped)
    if (/\.findAll\s*\(\s*\)/.test(line) && f.includes("repository"))
      add(f, "repository-findAll", n, line);

    // Rule 9: no native query with select *
    if (/@Query/.test(line) && /select\s+\*/i.test(line)) add(f, "native-select-star", n, line);

    // Rule 10: no wildcard package scan
    if (/"com\.pg\.\*"/.test(line)) add(f, "wildcard-package-scan", n, line);

    // Rule 11: no unlicensed fetch of a user-supplied URL without the allow-list
    if (/Jsoup\.connect\s*\(/.test(line) && !line.includes("allowList"))
      add(f, "unvalidated-url-fetch", n, line);

    // Rule 12: no hardcoded bearer/authorization header value
    if (/"Bearer\s+[A-Za-z0-9._-]{10,}"/.test(line)) add(f, "hardcoded-bearer", n, line);

    // Rule 14: the application is open source. No licence gating of any kind may
    // reappear: no machine fingerprinting, no licence token, no licence key.
    if (/\bgetHardwareAddress\s*\(/.test(line)) add(f, "machine-fingerprint", n, line);
    if (/\bNetworkInterface\b/.test(line)) add(f, "machine-fingerprint", n, line);
    if (/\bJWT\.decode\s*\(/.test(line)) add(f, "license-token-decode", n, line);
    if (/import\s+com\.auth0\.jwt/.test(line)) add(f, "license-token-decode", n, line);
    if (/\blicen[cs]e[-_.]?(key|token|check|validat)/i.test(line)) add(f, "license-gating", n, line);
  });
}

// Rule 14b: no licence or mac property in any configuration file
for (const f of [
  ...walk(join(ROOT, "src", "main", "resources"), (p) => /\.(yml|yaml|properties)$/.test(p)),
  ...walk(join(ROOT, "src", "test", "resources"), (p) => /\.(yml|yaml|properties)$/.test(p)),
]) {
  readFileSync(f, "utf8")
    .split(/\r?\n/)
    .forEach((line, i) => {
      if (/^\s*#/.test(line)) return;
      if (/^\s*(licen[cs]e|mac|license-key|activation)\s*[:=]/i.test(line))
        add(f, "license-config-key", i + 1, line);
    });
}

// Rule 13: no secret-shaped field on a JPA entity without @JsonIgnore
for (const f of javaFiles.filter((x) => x.includes(`${"domain"}/`) || x.includes(`${"domain"}\\`))) {
  const src = readFileSync(f, "utf8");
  const entityBlock = src.split(/\n\s*(?:@Entity|public class)/).slice(1).join("\n");
  const secretField =
    /private\s+String\s+(password|secret|refreshToken|accessToken|apiKey|clientSecret)\s*;/i.exec(entityBlock);
  if (secretField) {
    const around = entityBlock.slice(
      Math.max(0, secretField.index - 200),
      secretField.index + 200
    );
    if (!/@JsonIgnore/.test(around)) add(f, "secret-field-without-JsonIgnore", 0, secretField[0]);
  }
}

if (findings.length) {
  console.error(`HYGIENE-FAIL: ${findings.length} rule violation(s)`);
  for (const f of findings) console.error(`  ${f.file}:${f.line}  [${f.rule}]  ${f.text}`);
  process.exit(1);
}
console.log(`HYGIENE-PASS: ${javaFiles.length} java files, 0 violations`);
