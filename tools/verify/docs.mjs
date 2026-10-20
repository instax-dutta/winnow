#!/usr/bin/env node
/**
 * docs.mjs - check that the documentation set exists, is substantial, and covers the
 * subjects it is supposed to cover.
 *
 * Usage: node tools/verify/docs.mjs
 * Exit 0 and print "DOCS-PASS" when the documentation is complete.
 */
import { readFileSync, existsSync } from "node:fs";
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
    path: "docs/SECURITY.md",
    minLines: 120,
    mustMention: ["BCrypt", "CSRF", "SSRF", "secret store", "tenant", "open source"],
  },
  {
    path: "docs/MAINTENANCE.md",
    minLines: 120,
    mustMention: ["docker compose", "mvnw", "GATES.md", "Flyway", "migrations", "SEO_BUILD_HOST"],
  },
  {
    path: "docs/EXPLAINER.html",
    minLines: 200,
    // The explainer is a plain-language page. What is checked about it is that it is
    // complete and self-contained: long enough to carry the walkthrough, with balanced
    // tags, no script and no external resource.
    mustMention: [],
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
  if (doc.path.endsWith(".html")) {
    // A self-contained page: no external stylesheet, script or font, and no <script>.
    const external = src.match(/(?:src|href)="https?:\/\//g) || [];
    if (external.length) findings.push(`${doc.path}: ${external.length} external resource reference(s)`);
    if (/<script/i.test(src)) findings.push(`${doc.path}: contains a <script> tag`);
    const opens = (src.match(/<(div|section|table|ul|ol|li|td|tr|th|pre|p|h[1-4])\b/g) || []).length;
    const closes = (src.match(/<\/(div|section|table|ul|ol|li|td|tr|th|pre|p|h[1-4])>/g) || []).length;
    if (opens !== closes) findings.push(`${doc.path}: ${opens} open and ${closes} close tags`);
    const ids = new Set([...src.matchAll(/id="([a-z]+)"/g)].map((m) => m[1]));
    for (const m of src.matchAll(/href="#([a-z]+)"/g)) {
      if (!ids.has(m[1])) findings.push(`${doc.path}: a link points at the missing anchor #${m[1]}`);
    }
  }
  for (const topic of doc.mustMention) {
    if (!src.toLowerCase().includes(topic.toLowerCase())) {
      findings.push(`${doc.path}: does not mention "${topic}"`);
    }
  }
}

if (findings.length) {
  console.error(`DOCS-FAIL: ${findings.length} issue(s)`);
  for (const f of findings) console.error(`  ${f}`);
  process.exit(1);
}
console.log(`DOCS-PASS: ${REQUIRED.length} documents checked, all present and complete`);
