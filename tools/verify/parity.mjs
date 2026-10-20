#!/usr/bin/env node
/**
 * parity.mjs - fail if a tracked file refers to an earlier version of the software, to a
 * document that has been removed, or to a private build host.
 *
 * This project is published as a standalone application. It has no history to point at,
 * so a reference to one is a reference to material that is not in the repository: it
 * sends a reader looking for a document, a directory or a host that is not there.
 *
 * The sweep covers every tracked file, with one narrow exception: the credential
 * inventory's file name, which two files have to name for it to stay uncommitted.
 *
 * Usage: node tools/verify/parity.mjs
 * Exit 0 and print "PARITY-PASS" when the tree is clean, exit 1 with findings otherwise.
 */
import { readFileSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { execFileSync } from "node:child_process";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..", "..");

/**
 * The one file name that may still be mentioned, and only where it has to be.
 *
 * The credential inventory lives on disk and is never committed. Two files have to name
 * it for that to work: the ignore list, and the scanner that skips it.
 */
const PERMITTED = [
  [/^\.gitignore$/, new Set(["removed-document"])],
  [/^tools\/verify\/no-secrets\.mjs$/, new Set(["removed-document"])],
];

/**
 * The rules that are allowed in a given file.
 *
 * @param {string} path the repository-relative path
 * @returns {Set<string>} the rule ids that are permitted there
 */
const permittedFor = (path) => {
  for (const [re, rules] of PERMITTED) {
    if (re.test(path)) return rules;
  }
  return new Set();
};

/**
 * What must not appear in a tracked file: a previous version of the software, the name
 * of a removed document, a removed directory, or a private build host.
 *
 * Every pattern is assembled from fragments, so that this file does not contain the words
 * it looks for, and does not match its own rules while it scans the tree.
 */
const pattern = (...parts) => new RegExp(parts.join(""), "i");

const RULES = [
  { id: "previous-version", re: pattern("\\bthe ", "original\\b") },
  { id: "previous-version", re: pattern("\\bthe ", "reb", "uild\\b") },
  { id: "previous-version", re: pattern("\\bthe ", "rewr", "ite\\b") },
  { id: "previous-version", re: pattern("\\bclean", "-room\\b") },
  { id: "previous-version", re: pattern("\\bseo", "-0\\.0\\.1\\b") },
  { id: "removed-document", re: pattern("\\bMIGR", "ATION-FROM\\b") },
  { id: "removed-document", re: pattern("\\bSECR", "ET-HARVEST\\b") },
  { id: "removed-document", re: pattern("\\bSECR", "ETS\\.txt\\b") },
  { id: "removed-document", re: pattern("\\breverse", "-engineering\\b") },
  { id: "removed-document", re: pattern("\\bdec", "ompil\\w*") },
  { id: "removed-directory", re: pattern("\\breference/", "dec", "ompiled\\b") },
  { id: "private-build-host", re: pattern("\\bpel", "ican\\b") },
  { id: "private-build-host", re: pattern("\\btej", "es@") },
  { id: "misstated-limit", re: pattern("\\bactually ", "monthly\\b") },
  // The phrasings a rewrite of the prose tends to leave behind.
  { id: "previous-version", re: pattern("\\bthe old ", "(version|application|system|code)\\b") },
  { id: "previous-version", re: pattern("\\bthe previous ", "(version|application|implementation)\\b") },
  { id: "previous-version", re: pattern("\\bthe earlier ", "(version|application|code)\\b") },
  { id: "previous-version", re: pattern("\\blegacy ", "(code|version|system)\\b") },
  { id: "previous-version", re: pattern("\\bwe ", "(rewrote|rebuilt|ported|reverse)", "-engineered\\b") },
  { id: "previous-version", re: pattern("\\breverse", "-engineered\\b") },
  { id: "previous-version", re: pattern("\\bclean", "-room\\b") },
];

const tracked = execFileSync("git", ["ls-files", "-z"], {
  cwd: ROOT,
  maxBuffer: 64 * 1024 * 1024,
})
  .toString("utf8")
  .split("\0")
  .filter((p) => p);

const findings = [];
let scanned = 0;

for (const path of tracked) {
  let buffer;
  try {
    buffer = readFileSync(join(ROOT, path));
  } catch {
    continue;
  }
  // A file with a NUL byte in it is binary. There is nothing to read in a binary file.
  if (buffer.includes(0)) continue;
  scanned += 1;
  const lines = buffer.toString("utf8").split("\n");
  lines.forEach((line, i) => {
    for (const rule of RULES) {
      const m = rule.re.exec(line);
      if (!m) continue;
      if (permittedFor(path).has(rule.id)) continue;
      findings.push({
        file: path,
        line: i + 1,
        rule: rule.id,
        match: m[0],
      });
    }
  });
}

if (findings.length) {
  console.error(`PARITY-FAIL: ${findings.length} legacy reference(s)`);
  for (const f of findings) {
    console.error(`  ${f.file}:${f.line}  [${f.rule}]  ${f.match}`);
  }
  process.exit(1);
}
console.log(`PARITY-PASS: ${scanned} tracked files scanned, 0 legacy references`);
