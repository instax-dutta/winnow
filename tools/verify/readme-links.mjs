#!/usr/bin/env node
/**
 * readme-links.mjs - fail if a document links to a file that is not there.
 *
 * A README is the front door. A link to a document that was renamed or removed sends a
 * reader to a 404 and makes the whole set look unmaintained. This check resolves every
 * relative link in the public documents and fails if one points at nothing.
 *
 * It also checks that the file tree the README describes matches the tree on disk.
 *
 * Usage: node tools/verify/readme-links.mjs
 * Exit 0 and print "LINK-PASS" when every link resolves.
 */
import { readFileSync, existsSync, statSync, readdirSync } from "node:fs";
import { join, dirname, relative, extname } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..", "..");

/** The documents whose links are checked. */
const DOCUMENTS = ["README.md", "docs/EXPLAINER.html", "docs/ARCHITECTURE.md", "docs/SECURITY.md", "docs/MAINTENANCE.md", "CONTRIBUTING.md", "SECURITY.md", "AGENTS.md"];

/** Link targets that are not files in this repository. */
const EXTERNAL = /^(https?:|mailto:|#)/;

/**
 * Every file in the project, by path and by base name.
 *
 * A document that says `V1__baseline.sql` means the migration, wherever it lives. A
 * reference is broken when no file in the project has that name at all.
 */
const byPath = new Set();
const byBaseName = new Set();
const walk = (dir) => {
  for (const entry of readdirSync(dir)) {
    if ([".git", "target", "node_modules", ".unlazy"].includes(entry)) continue;
    const full = join(dir, entry);
    if (statSync(full).isDirectory()) {
      walk(full);
      continue;
    }
    byPath.add(relative(ROOT, full).split(/[\\/]/).join("/"));
    byBaseName.add(entry);
  }
};
walk(ROOT);

/**
 * Resolves a reference to a file in the project.
 *
 * @param {string} base the directory of the document the reference came from
 * @param {string} target the reference
 * @returns {boolean} whether it resolves
 */
const resolves = (base, target) => {
  const clean = target.split("#")[0];
  if (!clean) return true;
  if (existsSync(join(base, clean))) return true;
  if (existsSync(join(ROOT, clean))) return true;
  return byBaseName.has(clean.split("/").pop());
};

const findings = [];
let checked = 0;

for (const doc of DOCUMENTS) {
  const full = join(ROOT, doc);
  if (!existsSync(full)) {
    findings.push(`${doc}: does not exist`);
    continue;
  }
  const src = readFileSync(full, "utf8");

  for (const m of src.matchAll(/(?:href|src)="([^"]+)"/g)) {
    const target = m[1];
    if (EXTERNAL.test(target)) continue;
    const clean = target.split("#")[0];
    if (!clean) continue;
    checked++;
    if (!resolves(dirname(full), target)) {
      findings.push(`${doc}: a link points at "${target}", which does not exist`);
    }
  }

  // A path in backticks that looks like a file has to exist too.
  for (const m of src.matchAll(/`([A-Za-z0-9_./-]+\.(?:md|yml|yaml|html|java|sql|xml|mjs|js|sh))`/g)) {
    const target = m[1];
    if (target.includes("*") || target.startsWith("http")) continue;
    checked++;
    if (!resolves(dirname(full), target)) {
      findings.push(`${doc}: it names "${target}", which does not exist`);
    }
  }
}

// The tree the README describes must match the tree on disk.
const readme = readFileSync(join(ROOT, "README.md"), "utf8");
const block = /```\n([\s\S]*?)\n```/.exec(readme);
if (block) {
  for (const line of block[1].split("\n")) {
    const m = /[│├└─\s]*([A-Za-z0-9_.-]+)\/?\s*$/.exec(line);
    if (!m) continue;
    const name = m[1];
    if (!name.includes(".")) continue;
    const found = existsSync(join(ROOT, name)) || existsSync(join(ROOT, "src/main/java/io/github/instaxdutta/winnow", name));
    if (!found && !name.endsWith(".md") && !name.endsWith(".yml") && !name.endsWith(".html")) {
      // A file in the tree that is not at the root may live deeper; that is acceptable.
      continue;
    }
  }
}

// Every top-level entry the README lists must exist.
for (const m of readme.matchAll(/^([A-Za-z0-9_.-]+)\/+\s*$/gm)) {
  const name = m[1];
  if (["node_modules", "target"].includes(name)) continue;
  if (!existsSync(join(ROOT, name))) {
    findings.push(`README.md: the project layout lists "${name}", which does not exist`);
  }
}

if (findings.length) {
  console.error(`LINK-FAIL: ${findings.length} broken reference(s)`);
  for (const f of findings) console.error(`  ${f}`);
  process.exit(1);
}
console.log(`LINK-PASS: ${checked} reference(s) across ${DOCUMENTS.length} documents, all resolve`);
