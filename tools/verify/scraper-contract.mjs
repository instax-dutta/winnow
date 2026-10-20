#!/usr/bin/env node
/**
 * scraper-contract.mjs - prove the Amazon sidecar keeps its contract.
 *
 * The sidecar is a separate service, so the two halves of the contract can drift apart.
 * The sidecar's own verification proves the service half without a browser; this wrapper
 * runs it from the repository root, so the check is one command in the ledger.
 *
 * Usage: node tools/verify/scraper-contract.mjs
 * Exit 0 and print "SCRAPER-PASS" when the contract holds.
 */
import { execFileSync } from "node:child_process";
import { existsSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..", "..");
const SCRAPER = join(ROOT, "scraper");

const say = (m) => console.log(`[scraper] ${m}`);

if (!existsSync(join(SCRAPER, "package.json"))) {
  console.error("SCRAPER-FAIL: the scraper directory is missing or is not a Node project");
  process.exit(1);
}

// The browser download is not needed for the contract proof, and it is slow.
if (!existsSync(join(SCRAPER, "node_modules"))) {
  say("installing the dependencies");
  execFileSync("npm", ["ci", "--ignore-scripts"], { cwd: SCRAPER, stdio: "inherit" });
}

say("running the sidecar verification");
const out = execFileSync("npm", ["test"], { cwd: SCRAPER, encoding: "utf8", maxBuffer: 16 * 1024 * 1024 });
const lines = out.trim().split("\n");
for (const line of lines.slice(-12)) {
  console.log(`  ${line}`);
}

if (!out.includes("VERIFY-PASS")) {
  console.error("SCRAPER-FAIL: the sidecar verification did not report VERIFY-PASS");
  process.exit(1);
}
console.log("SCRAPER-PASS: the sidecar keeps its contract");
