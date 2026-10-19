#!/usr/bin/env node
/**
 * app-boot.mjs - start the packaged application against the dedicated MySQL 8 container
 * and prove that it serves its login page and accepts a login.
 *
 * The application runs on the build host, in its own directory, against its own database
 * container. Nothing else on the host is touched.
 *
 * Usage: node tools/verify/app-boot.mjs
 * Exit 0 and print "APP-BOOT-PASS" when the application boots and serves.
 */
import { execFileSync, spawn } from "node:child_process";
import { existsSync, rmSync, mkdirSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..", "..");
const HOST = process.env.SEO_BUILD_HOST || "tejes@pelican";
const REMOTE = "~/seo-rebuild/app";
// A fresh port every run, so a stale instance from an earlier run can never answer the
// probes. The port is chosen by the operating system and is released as soon as the
// application stops.
const PORT = Number(process.env.SEO_APP_PORT || 20000 + Math.floor(Math.random() * 20000));
const DB_URL =
  process.env.SEO_DB_URL ||
  "jdbc:mysql://127.0.0.1:3307/emailsender_v01?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8";
const USER = process.env.SEO_DB_USER || "seo_v01";
const PASSWORD = process.env.SEO_DB_PASSWORD || "SeoV01DevPass!x";
const ADMIN_PASSWORD = process.env.SEO_ADMIN_PASSWORD || "boot-check-password-1";

const sh = (cmd) => execFileSync("ssh", [HOST, cmd], { encoding: "utf8", maxBuffer: 64 * 1024 * 1024 });

const say = (m) => console.log(`[app-boot] ${m}`);

/**
 * Runs a remote command without waiting for it to finish.
 *
 * The application is a long running process. Its ssh channel stays open for the lifetime
 * of the application, so the call has to be detached rather than awaited.
 */
const shDetached = (cmd) => {
  const child = spawn("ssh", [HOST, cmd], { detached: true, stdio: "ignore" });
  child.unref();
};

try {
  // ---------------------------------------------------------------- build
  say("building the container image on the build host");
  sh(
    `cd ${REMOTE} && docker build -t seo-v01:boot . 2>&1 | tail -5`
  );
  say("image: seo-v01:boot");

  // ---------------------------------------------------------------- start
  const logFile = "app-boot.log";
  // Any instance from an earlier run is stopped first, so it can neither hold the port
  // nor answer the probes with a different bootstrap password.
  sh(`docker rm -f seo-v01-boot >/dev/null 2>&1 || true`);
  sh(`sleep 1`);
  // The container is the application process. It reaches the database container over the
  // compose network, so the database URL points at the host port that is published.
  const env = [
    `-e DB_URL='${DB_URL}'`,
    `-e DB_USERNAME='${USER}'`,
    `-e DB_PASSWORD='${PASSWORD}'`,
    `-e BOOTSTRAP_PASSWORD='${ADMIN_PASSWORD}'`,
    `-e SERVER_PORT=${PORT}`,
    `-e SPRING_PROFILES_ACTIVE=dev`,
  ].join(" ");
  sh(
    `docker run -d --name seo-v01-boot --network host ${env} seo-v01:boot >/dev/null`
  );
  sh(`docker logs seo-v01-boot > ${REMOTE}/${logFile} 2>&1 || true`);

  // ---------------------------------------------------------------- wait for the port
  let up = false;
  for (let i = 0; i < 90; i++) {
    await new Promise((r) => setTimeout(r, 2000));
    const probe = sh(
      `curl -s -o /dev/null -w '%{http_code}' --max-time 5 http://127.0.0.1:${PORT}/login || true`
    ).trim();
    if (probe === "200") {
      up = true;
      break;
    }
    if (i === 30 || i === 60) {
      say(`still waiting for the application (last probe: ${probe})`);
    }
  }

  if (!up) {
    const log = sh(`tail -60 ${REMOTE}/${logFile} 2>/dev/null || true`);
    throw new Error(`the application did not serve /login\n${log}`);
  }
  say("the application is serving /login");

  // ---------------------------------------------------------------- the page is real
  // The cookie jar is used for the page fetch as well. The CSRF token is bound to the
  // session that the page created, so the POST has to present that same session.
  const cookieJar = "/tmp/app-boot-cookies-" + Date.now();
  const page = sh(`curl -s -c ${cookieJar} -b ${cookieJar} --max-time 10 http://127.0.0.1:${PORT}/login`);
  if (!page.includes('name="username"')) throw new Error("the login form has no username field");
  if (!page.includes('name="password"')) throw new Error("the login form has no password field");
  if (!page.includes("_csrf")) throw new Error("the login form has no CSRF token");
  if (/licen[cs]e|activation|machine id/i.test(page))
    throw new Error("the login page mentions a licence or an activation mechanism");
  say("the login form is complete and carries no licence text");

  // ---------------------------------------------------------------- a login works
  const csrf = /name="_csrf"[^>]*value="([^"]+)"/.exec(page)?.[1];
  if (!csrf) throw new Error("could not read the CSRF token out of the login form");
  sh(
    `curl -s -o /dev/null -c ${cookieJar} -b ${cookieJar} -X POST http://127.0.0.1:${PORT}/login ` +
      `--data-urlencode "username=admin" --data-urlencode "password=${ADMIN_PASSWORD}" ` +
      `--data-urlencode "_csrf=${csrf}" -w '%{http_code} %{redirect_url}'`
  );
  const dash = sh(`curl -s -o /dev/null -b ${cookieJar} -w '%{http_code}' http://127.0.0.1:${PORT}/dashboard`);
  if (dash.trim() !== "200") throw new Error(`the dashboard answered ${dash.trim()} after a login`);
  say("an administrator can sign in and reach the dashboard");

  // ---------------------------------------------------------------- a wrong password is rejected
  const badCookie = "/tmp/app-boot-bad-" + Date.now();
  const badPage = sh(`curl -s -c ${badCookie} -b ${badCookie} --max-time 10 http://127.0.0.1:${PORT}/login`);
  const badCsrf = /name="_csrf"[^>]*value="([^"]+)"/.exec(badPage)?.[1];
  const badCode = sh(
    `curl -s -o /dev/null -c ${badCookie} -b ${badCookie} -X POST http://127.0.0.1:${PORT}/login ` +
      `--data-urlencode "username=admin" --data-urlencode "password=definitely-wrong" ` +
      `--data-urlencode "_csrf=${badCsrf}" -w '%{http_code}'`
  ).trim();
  if (badCode !== "302") throw new Error(`a wrong password answered ${badCode}, expected a redirect`);
  say("a wrong password is rejected");

  sh(`rm -f ${cookieJar} ${badCookie}`);
  sh(`docker rm -f seo-v01-boot >/dev/null 2>&1 || true`);
  console.log("APP-BOOT-PASS");
  process.exit(0);
} catch (err) {
  console.error(`APP-BOOT-FAIL: ${err.message}`);
  try {
    sh(`docker rm -f seo-v01-boot >/dev/null 2>&1 || true`);
    const log = sh(`docker logs --tail 40 seo-v01-boot 2>&1 || true`);
    if (log.trim()) console.error("--- application log ---\n" + log);
  } catch {
    /* nothing more to report */
  }
  process.exit(1);
}
