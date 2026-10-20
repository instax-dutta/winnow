#!/usr/bin/env node
/**
 * clean-build.mjs - prove that a fresh clone of the repository builds, with no local
 * Maven cache and nothing carried over from the working tree.
 *
 * The repository is copied to a scratch directory on the build host, a fresh local Maven
 * repository is used, and the full verify runs. A build that only works because of a
 * stale target directory or a warm cache fails here.
 *
 * Usage: node tools/verify/clean-build.mjs
 * Exit 0 and print "CLEAN-BUILD-PASS" when the build succeeds.
 */
import { execFileSync } from "node:child_process";
import { existsSync, rmSync, mkdirSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { execFileSync as run } from "node:child_process";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..", "..");
// The build host and the directory this project owns on it. Both come from the
// environment: a gate runs on whatever host the operator names. The scratch directories
// are derived from it, so they stay inside this project's own footprint.
const HOST = process.env.SEO_BUILD_HOST || "";
const REMOTE = process.env.SEO_BUILD_DIR || "";
const SCRATCH = `${REMOTE}-clean-check`;
const M2 = `${REMOTE}-clean-m2`;

if (!HOST || !REMOTE) {
  console.error("CLEAN-BUILD-FAIL: the build host is not configured.");
  console.error("Set SEO_BUILD_HOST to the machine the build runs on and SEO_BUILD_DIR to the");
  console.error("directory this project owns on it.");
  process.exit(1);
}

const sh = (cmd) => execFileSync("ssh", [HOST, cmd], { encoding: "utf8", maxBuffer: 128 * 1024 * 1024 });
const say = (m) => console.log(`[clean-build] ${m}`);

try {
  // ---------------------------------------------------------------- the copy
  say("copying the working tree to a scratch directory, without the build output");
  const tar = execFileSync(
    "tar",
    [
      "czf",
      "-",
      "--no-xattrs",
      "--exclude=.git",
      "--exclude=target",
      "--exclude=node_modules",
      "--exclude=.unlazy",
      "-C",
      ROOT,
      ".",
    ],
    { maxBuffer: 128 * 1024 * 1024 }
  );
  sh(`rm -rf ${SCRATCH} ${M2} && mkdir -p ${SCRATCH} ${M2}`);
  // execFileSync throws when the command exits non-zero, so reaching this line means the
  // copy worked.
  run("ssh", [HOST, `tar xzf - -C ${SCRATCH}`], {
    input: tar,
    maxBuffer: 128 * 1024 * 1024,
    stdio: ["pipe", "pipe", "pipe"],
  });

  // ---------------------------------------------------------------- the build
  say("building with a fresh local Maven repository (this downloads every dependency again)");
  const out = sh(
    `cd ${SCRATCH} && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ` +
      `./mvnw -B -Dmaven.repo.local=${M2} verify 2>&1 | tail -40`
  );
  if (!/BUILD SUCCESS/.test(out)) {
    throw new Error(`the clean build did not succeed\n${out}`);
  }
  const tests = /Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)/g;
  let last = null;
  for (const m of out.matchAll(tests)) last = m;
  say(`build succeeded${last ? `, ${last[1]} tests, ${last[2]} failures, ${last[3]} errors` : ""}`);

  // ---------------------------------------------------------------- the artifact
  const jar = sh(`ls ${SCRATCH}/target/*.jar 2>/dev/null | head -1`).trim();
  if (!jar.endsWith(".jar")) throw new Error(`the build produced no jar: ${jar}`);
  const size = sh(`stat -c %s ${jar}`).trim();
  say(`artifact: ${jar} (${Math.round(Number(size) / 1024 / 1024)} MB)`);

  sh(`rm -rf ${SCRATCH} ${M2}`);
  console.log("CLEAN-BUILD-PASS");
  process.exit(0);
} catch (err) {
  console.error(`CLEAN-BUILD-FAIL: ${err.message}`);
  try {
    sh(`rm -rf ${SCRATCH} ${M2}`);
  } catch {
    /* nothing more to report */
  }
  process.exit(1);
}
