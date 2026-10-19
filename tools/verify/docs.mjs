#!/usr/bin/env node
/**
 * docs.mjs - check that the documentation set exists, is substantial, and covers the
 * subjects it is supposed to cover.
 *
 * Usage: node tools/verify/docs.mjs
 * Exit 0 and print "DOCS-PASS" when the documentation is complete.
 */
import { readFileSync, existsSync, statSync, readdirSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..", "..");

/** The documents, and the subjects each one has to mention. */
const REQUIRED = [
  {
    path: "README.md",
    minLines: 80,
    mustMention: ["docker compose", "BOOTSTRAP_PASSWORD", "SECRET_STORE_PATH", "mvnw", "licence"],
  },
  {
    path: "docs/ARCHITECTURE.md",
    minLines: 150,
    mustMention: [
      "web",
      "service",
      "repository",
      "domain",
      "Flyway",
      "tenant",
      "extraction",
      "outbox",
      "seo.mail",
      "seo.extraction",
    ],
  },
  {
    path: "docs/MIGRATION-FROM-0.0.1.md",
    minLines: 120,
    mustMention: ["login", "client_master", "email_master", "gmail_master", "email_queue", "email_send_job"],
  },
  {
    path: "docs/SECURITY.md",
    minLines: 120,
    mustMention: ["BCrypt", "CSRF", "SSRF", "secret store", "tenant", "open source"],
  },
  {
    path: "docs/MAINTENANCE.md",
    minLines: 120,
    mustMention: ["pelican", "docker compose", "mvnw", "GATES.md", "Flyway", "migrations"],
  },
  {
    path: "docs/SECRET-HARVEST.md",
    minLines: 30,
    mustMention: ["CRITICAL", "GOCSPX", "rotate"],
  },
];

const findings = [];

for (const doc of REQUIRED) {
  const full = join(ROOT, doc.path);
  if (!existsSync(full)) {
    findings.push(`${doc.path}: does not exist`);
    continue;
  }
  const src = readFileSync(full, "utf8");
  const lines = src.split("\n").length;
  if (lines < doc.minLines) {
    findings.push(`${doc.path}: ${lines} lines, expected at least ${doc.minLines}`);
  }
  for (const topic of doc.mustMention) {
    if (!src.toLowerCase().includes(topic.toLowerCase())) {
      findings.push(`${doc.path}: does not mention "${topic}"`);
    }
  }
}

// The reverse engineering record has to stay complete: three analyses.
for (const name of [
  "docs/reverse-engineering/01-services-and-scraper.md",
  "docs/reverse-engineering/02-controllers-and-security.md",
  "docs/reverse-engineering/03-entities-and-templates.md",
]) {
  const full = join(ROOT, name);
  if (!existsSync(full)) {
    findings.push(`${name}: does not exist`);
    continue;
  }
  if (statSync(full).size < 8000) {
    findings.push(`${name}: is too small to be an analysis`);
  }
}

// The decompiled source has to be there. It is the evidence for every claim.
const decompiled = join(ROOT, "reference", "decompiled", "com", "pg", "seo");
if (!existsSync(decompiled)) {
  findings.push("reference/decompiled: the decompiled original is missing");
} else {
  let count = 0;
  const walk = (d) => {
    for (const e of readdirSync(d)) {
      const p = join(d, e);
      if (statSync(p).isDirectory()) walk(p);
      else if (p.endsWith(".java")) count++;
    }
  };
  walk(decompiled);
  if (count < 60) findings.push(`reference/decompiled: only ${count} classes, expected the full original`);
}

if (findings.length) {
  console.error(`DOCS-FAIL: ${findings.length} issue(s)`);
  for (const f of findings) console.error(`  ${f}`);
  process.exit(1);
}
console.log(`DOCS-PASS: ${REQUIRED.length} documents checked, all present and complete`);
