#!/usr/bin/env node
/**
 * dox-tree.mjs - verify the DOX tree is complete and internally consistent.
 *
 * Checks:
 *   1. Every AGENTS.md has the six required sections in the required order.
 *   2. Every "Child DOX Index" table entry points at a path that has an AGENTS.md.
 *   3. Every directory that contains an AGENTS.md is listed in its parent's index.
 *   4. No AGENTS.md is orphaned (unreachable from the root index).
 *
 * Usage: node tools/verify/dox-tree.mjs
 * Exit 0 and print "DOX-TREE-PASS" when consistent, exit 1 with findings otherwise.
 */
import { readFileSync, readdirSync, statSync, existsSync } from "node:fs";
import { join, dirname, relative, sep } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = join(fileURLToPath(import.meta.url), "..", "..", "..");
const rel = (p) => relative(ROOT, p).split(sep).join("/");

const SECTIONS = [
  "## Purpose",
  "## Ownership",
  "## Local Contracts",
  "## Work Guidance",
  "## Verification",
  "## Child DOX Index",
];

const SKIP_DIRS = new Set([".git", "target", "node_modules", "reference", ".unlazy"]);

const walk = (dir) => {
  const out = [];
  for (const e of readdirSync(dir)) {
    if (SKIP_DIRS.has(e)) continue;
    const p = join(dir, e);
    if (statSync(p).isDirectory()) out.push(...walk(p));
    else if (e === "AGENTS.md") out.push(p);
  }
  return out;
};

const all = walk(ROOT);
const findings = [];
const byDir = new Map(all.map((f) => [dirname(f), f]));

// 1. required sections, in order.
// The root is the DOX rail: it carries the framework contract plus the project
// sections, not the six-section child shape. Children must carry all six.
const ROOT_REQUIRED = ["## Child DOX Index"];
for (const f of all) {
  const src = readFileSync(f, "utf8");
  const isRoot = dirname(f) === ROOT;
  const required = isRoot ? ROOT_REQUIRED : SECTIONS;
  let last = -1;
  for (const s of required) {
    const idx = src.indexOf(s);
    if (idx < 0) {
      findings.push(`${rel(f)}: missing section "${s}"`);
      continue;
    }
    if (idx < last) findings.push(`${rel(f)}: section "${s}" is out of order`);
    last = idx;
  }
}

// 2 + 3. index entries resolve, and every child dir is indexed by its parent.
// Index entries are repository-relative so one table format works everywhere.
const indexEntries = new Map(); // parentDir -> Set(repo-relative child paths)
for (const f of all) {
  const src = readFileSync(f, "utf8");
  const dir = dirname(f);
  const set = new Set();
  const idx = src.indexOf("## Child DOX Index");
  const tail = idx >= 0 ? src.slice(idx) : "";
  for (const line of tail.split("\n")) {
    const m = /^\|\s*`([^`]+)`\s*\|/.exec(line);
    if (!m) continue;
    const child = m[1].replace(/\/$/, "");
    set.add(child);
    if (!existsSync(join(ROOT, child, "AGENTS.md")))
      findings.push(`${rel(f)}: index entry "${child}" has no AGENTS.md`);
  }
  indexEntries.set(dir, set);
}

// every dir with an AGENTS.md must be indexed by its nearest ancestor AGENTS.md
const nearestAncestorWithIndex = (dir) => {
  let p = dirname(dir);
  while (p !== dirname(p)) {
    if (byDir.has(p)) return p;
    p = dirname(p);
  }
  return null;
};
for (const dir of byDir.keys()) {
  if (dir === ROOT) continue;
  const parent = nearestAncestorWithIndex(dir);
  if (!parent) {
    findings.push(`${rel(dir)}: no ancestor AGENTS.md can index it`);
    continue;
  }
  const parentIndex = indexEntries.get(parent) || new Set();
  const childRel = rel(dir);
  if (!parentIndex.has(childRel))
    findings.push(`${rel(parent)}: does not index child "${childRel}"`);
}

// 4. reachability from root
const reachable = new Set();
const visit = (dir) => {
  if (reachable.has(dir)) return;
  reachable.add(dir);
  for (const child of indexEntries.get(dir) || []) visit(join(ROOT, child));
};
visit(ROOT);
for (const dir of byDir.keys()) {
  if (!reachable.has(dir)) findings.push(`${rel(dir)}: not reachable from the root index`);
}

if (findings.length) {
  console.error(`DOX-TREE-FAIL: ${findings.length} issue(s)`);
  for (const f of findings) console.error(`  ${f}`);
  process.exit(1);
}
console.log(`DOX-TREE-PASS: ${all.length} AGENTS.md files, sections and index consistent`);
