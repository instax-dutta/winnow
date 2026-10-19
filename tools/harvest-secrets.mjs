#!/usr/bin/env node
/**
 * harvest-secrets.mjs - Extract every hardcoded credential, key and token
 * from the original seo-0.0.1-SNAPSHOT artifact set.
 *
 * Scans four surfaces:
 *   1. BOOT-INF/classes/application.properties  - config secrets
 *   2. emailsend21.sql                          - seeded DB credentials
 *   3. decompiled source tree (java)            - literals in code
 *   4. templates/*.html + puppeteer/*.js        - front-end and scraper
 *
 * Usage: node tools/harvest-secrets.mjs
 * Writes: SECRETS.txt (repo root) and prints a summary to stdout.
 */
import { readFileSync, writeFileSync, existsSync, readdirSync, statSync } from "node:fs";
import { join, dirname, basename } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..");
const OUT = join(ROOT, "SECRETS.txt");

const SRC_JAR_DIR = "/Users/saiduttaabhishekdash/seo-0.0.1-SNAPSHOT";
const EXTRACTED = "/tmp/seo-re/work/extracted/BOOT-INF/classes";
const DECOMPILED = join(ROOT, "reference", "decompiled");

const walk = (dir, filter = () => true) => {
  if (!existsSync(dir)) return [];
  return readdirSync(dir).flatMap((e) => {
    const p = join(dir, e);
    return statSync(p).isDirectory() ? walk(p, filter) : filter(p) ? [p] : [];
  });
};

const S = []; // findings
const add = (cat, sev, label, value, where, note = "") =>
  S.push({ cat, sev, label, value, where, note });

// ---------------------------------------------------------------- 1. properties
const propsPath = join(EXTRACTED, "application.properties");
const props = new Map();
if (existsSync(propsPath)) {
  for (const line of readFileSync(propsPath, "utf8").split(/\r?\n/)) {
    const t = line.trim();
    if (!t || t.startsWith("#")) continue;
    const i = t.indexOf("=");
    if (i < 0) continue;
    props.set(t.slice(0, i).trim(), t.slice(i + 1).trim());
  }
}

const PROP_CLASS = {
  "app.datasource.mysql.password": ["CRITICAL", "MySQL root password (plaintext in source control)"],
  "app.datasource.mysql.username": ["HIGH", "MySQL username (root)"],
  "app.datasource.mysql.url": ["MEDIUM", "MySQL JDBC URL (internal host/port)"],
  license: ["CRITICAL", "HS256 license JWT - only the payload is decoded, signature is NEVER verified"],
  mac: ["HIGH", "Licence-locked MAC address of the original operator workstation"],
  "wordpress.api.token": ["CRITICAL", "WordPress REST API token"],
};

for (const [k, v] of props) {
  if (v.startsWith("$")) continue; // unexpanded placeholder
  const [sev, note] = PROP_CLASS[k] || ["MEDIUM", "Config value"];
  add("Config (application.properties)", sev, k, v, "BOOT-INF/classes/application.properties", note);
}

// ---------------------------------------------------------------- 2. SQL dump
const sqlPath = join(SRC_JAR_DIR, "emailsend21.sql");
if (existsSync(sqlPath)) {
  const sql = readFileSync(sqlPath, "utf8");

  // gmail_master: OAuth client id/secret + refresh + access tokens
  const gm = /insert\s+into\s+`gmail_master`\s*\([^)]*\)\s*values\s*([\s\S]*?);\s*\n/i.exec(sql);
  if (gm) {
    const rows = gm[1]
      .split(/\),\s*\n|\)\s*$/g)
      .map((r) => r.trim().replace(/^\(|\)$/g, ""))
      .map((r) => r.split(/,\s*(?=(?:[^']*'[^']*')*[^']*$)/).map((v) => v.trim().replace(/^'|'$/g, "")));
    for (const r of rows) {
      if (r.length < 6) continue;
      const [id, email, clientId, secret, refresh, access] = r;
      add("Google OAuth (gmail_master)", "CRITICAL", `OAuth clientId (gmailMasterId=${id})`, clientId, "emailsend21.sql:gmail_master", `Gmail account: ${email}`);
      add("Google OAuth (gmail_master)", "CRITICAL", `OAuth client secret (gmailMasterId=${id})`, secret, "emailsend21.sql:gmail_master", `Gmail account: ${email}`);
      if (refresh && refresh !== "NULL") add("Google OAuth (gmail_master)", "CRITICAL", `OAuth refresh token (gmailMasterId=${id})`, refresh, "emailsend21.sql:gmail_master", `Gmail account: ${email}`);
      if (access && access !== "NULL") add("Google OAuth (gmail_master)", "CRITICAL", `OAuth access token (gmailMasterId=${id})`, access, "emailsend21.sql:gmail_master", `Gmail account: ${email} - short lived, still a live secret`);
    }
  }

  // email_master: SMTP app passwords
  const em = /insert\s+into\s+`email_master`\s*\([^)]*\)\s*values\s*([\s\S]*?);\s*\n/i.exec(sql);
  if (em) {
    const rows = em[1]
      .split(/\),\s*\n/g)
      .map((r) => r.trim().replace(/^\(|\)$/g, ""))
      .map((r) => r.split(/,\s*(?=(?:[^']*'[^']*')*[^']*$)/).map((v) => v.trim().replace(/^'|'$/g, "")));
    for (const r of rows) {
      if (r.length < 9) continue;
      const [id, email, proto, host, user, port, , , password] = r;
      if (!password || password === "NULL") continue;
      add("SMTP account (email_master)", "CRITICAL", `SMTP app password (emailMasterId=${id})`, password, "emailsend21.sql:email_master", `${user} @ ${host}:${port} (${proto})`);
    }
  }

  // login: bcrypt hashes + PII
  const ln = /insert\s+into\s+`login`\s*\([^)]*\)\s*values\s*([\s\S]*?);\s*\n/i.exec(sql);
  if (ln) {
    const rows = ln[1]
      .split(/\),\s*\n/g)
      .map((r) => r.trim().replace(/^\(|\)$/g, ""))
      .map((r) => r.split(/,\s*(?=(?:[^']*'[^']*')*[^']*$)/).map((v) => v.trim().replace(/^'|'$/g, "")));
    for (const r of rows) {
      if (r.length < 9) continue;
      const [id, username, password, first, last, personal, dob, pan] = r;
      add("User account (login)", "HIGH", `bcrypt password hash (LoginId=${id})`, password, "emailsend21.sql:login", `user: ${username}`);
      if (pan && pan !== "NULL") add("User PII (login)", "MEDIUM", `PAN (LoginId=${id})`, pan, "emailsend21.sql:login", `user: ${username}`);
      if (dob && dob !== "NULL") add("User PII (login)", "MEDIUM", `DOB (LoginId=${id})`, dob, "emailsend21.sql:login", `user: ${username}`);
    }
  }

  // role table
  const rl = /insert\s+into\s+`role_security`\s*\([^)]*\)\s*values\s*([\s\S]*?);\s*\n/i.exec(sql);
  if (rl) add("Authorization model (role_security)", "LOW", "Role definitions", rl[1].replace(/\s+/g, " ").trim().slice(0, 300), "emailsend21.sql:role_security", "Roles drive the SecurityConfiguration matchers");

  // client_master - business client contacts
  const cm = /insert\s+into\s+`client_master`\s*\([^)]*\)\s*values\s*([\s\S]*?);\s*\n/i.exec(sql);
  if (cm) add("Client contacts (client_master)", "LOW", "Seeded client rows", String(cm[1].split(/\),\s*\n/).length) + " rows (emails + web links)", "emailsend21.sql:client_master", "Business contact data");

  // scraped lead corpus
  const ed = /insert\s+into\s+`email_extractor_data`\s*\([^)]*\)\s*values\s*([\s\S]*?);\s*\n/i.exec(sql);
  if (ed) {
    const emails = [...ed[1].matchAll(/'([^']+@[^']+)'/g)].map((m) => m[1]);
    add("Scraped lead corpus (email_extractor_data)", "MEDIUM", "Scraped contact emails", `${emails.length} scraped email addresses`, "emailsend21.sql:email_extractor_data", `e.g. ${emails.slice(0, 3).join(", ")}`);
  }
}

// ---------------------------------------------------------------- 3. decompiled Java
const secretPatterns = [
  [/\bGOCSPX-[A-Za-z0-9_-]{20,}/g, "CRITICAL", "Google OAuth client secret literal"],
  [/\b\d{6,}-[0-9a-z]{20,}\.apps\.googleusercontent\.com/g, "CRITICAL", "Google OAuth client id literal"],
  [/\beyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}/g, "CRITICAL", "JWT literal"],
  [/\b1\/\/[0-9A-Za-z_-]{40,}/g, "CRITICAL", "Google OAuth refresh token literal"],
  [/\bya29\.[A-Za-z0-9_-]{30,}/g, "CRITICAL", "Google OAuth access token literal"],
  [/\bAIza[0-9A-Za-z_-]{35}\b/g, "CRITICAL", "Google API key literal"],
  [/\bAKIA[0-9A-Z]{16}\b/g, "CRITICAL", "AWS access key id literal"],
  [/\b(?:sk|pk)_(?:live|test)_[0-9A-Za-z]{16,}\b/g, "CRITICAL", "Stripe-style key literal"],
  [/\b[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\b/g, "LOW", "IP address literal"],
];

for (const f of walk(DECOMPILED, (p) => p.endsWith(".java"))) {
  const src = readFileSync(f, "utf8");
  const rel = f.slice(ROOT.length + 1);
  for (const [re, sev, label] of secretPatterns) {
    for (const m of src.matchAll(re)) {
      add("Hardcoded in code", sev, label, m[0], rel, "Literal embedded in compiled class");
    }
  }
  // hardcoded credentials assigned in code
  for (const m of src.matchAll(/"((?:password|passwd|pwd|secret|token|apikey|api_key|auth)\w*)"\s*[,)]/gi)) {
    add("Hardcoded in code", "LOW", `Credential-shaped field name: ${m[1]}`, m[1], rel, "Field name only - value is DB/config driven");
  }
}

// ---------------------------------------------------------------- 4. templates + puppeteer
const frontPatterns = [
  [/\bGOCSPX-[A-Za-z0-9_-]{20,}/g, "CRITICAL", "Google OAuth client secret literal"],
  [/\beyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}/g, "CRITICAL", "JWT literal"],
  [/\bAIza[0-9A-Za-z_-]{35}\b/g, "CRITICAL", "Google API key literal"],
];

for (const f of [...walk(join(EXTRACTED, "templates"), (p) => p.endsWith(".html")), ...walk(join(EXTRACTED, "puppeteer"), (p) => p.endsWith(".js") && !p.includes("node_modules"))]) {
  const src = readFileSync(f, "utf8");
  const rel = f.replace(EXTRACTED, "BOOT-INF/classes");
  for (const [re, sev, label] of frontPatterns) {
    for (const m of src.matchAll(re)) add("Hardcoded in front-end", sev, label, m[0], rel, "Literal embedded in shipped asset");
  }
}

// ---------------------------------------------------------------- report
const order = { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3 };
S.sort((a, b) => order[a.sev] - order[b.sev] || a.cat.localeCompare(b.cat) || a.label.localeCompare(b.label));

const counts = S.reduce((m, s) => ((m[s.sev] = (m[s.sev] || 0) + 1), m), {});
const bar = "=".repeat(78);

const lines = [];
lines.push(bar);
lines.push("HARVESTED SECRETS / CREDENTIALS / KEYS");
lines.push("Source artifact : seo-0.0.1-SNAPSHOT.jar (Spring Boot 3.3.0, com.pg.seo.EmailSenderApplication)");
lines.push("Compiled         : 2025-08-18  |  Build-Jdk 17  |  app version 0.0.1-SNAPSHOT");
lines.push(`Generated        : ${new Date().toISOString()}`);
lines.push(`Total findings   : ${S.length}  (CRITICAL ${counts.CRITICAL || 0} | HIGH ${counts.HIGH || 0} | MEDIUM ${counts.MEDIUM || 0} | LOW ${counts.LOW || 0})`);
lines.push(bar);
lines.push("");
lines.push("HOW TO READ");
lines.push("  CRITICAL  Rotate / revoke immediately. Granting access or cost to a third party.");
lines.push("  HIGH      Rotate before any reuse. Hard to rotate or tied to a person/machine.");
lines.push("  MEDIUM    Personal or business data. Treat as a data-protection obligation.");
lines.push("  LOW       Structural information. Useful for understanding the build.");
lines.push("");
lines.push("SEVERITY LEGEND");
lines.push("  CRITICAL  CRITICAL    HIGH  HIGH    MEDIUM  MEDIUM    LOW  LOW");
lines.push("");

let lastCat = null;
for (const s of S) {
  if (s.cat !== lastCat) {
    lines.push("");
    lines.push("-".repeat(78));
    lines.push(`## ${s.cat}`);
    lines.push("-".repeat(78));
    lastCat = s.cat;
  }
  lines.push("");
  lines.push(`[${s.sev}] ${s.label}`);
  lines.push(`    value : ${s.value}`);
  lines.push(`    where : ${s.where}`);
  if (s.note) lines.push(`    note  : ${s.note}`);
}

lines.push("");
lines.push(bar);
lines.push("END OF HARVEST");
lines.push(bar);

writeFileSync(OUT, lines.join("\n"), "utf8");
console.log(`Wrote ${OUT}`);
console.log(`${S.length} findings: ${JSON.stringify(counts)}`);
for (const s of S.filter((x) => x.sev === "CRITICAL")) console.log(`  CRITICAL  ${s.label}`);
