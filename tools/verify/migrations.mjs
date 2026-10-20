#!/usr/bin/env node
/**
 * migrations.mjs - prove the Flyway migrations apply to a clean MySQL 8 and that the
 * resulting schema is the one the entities expect.
 *
 * The check is deliberately strong: the database is dropped, the application is booted
 * against it, and the boot runs Flyway and then Hibernate's schema validation. If the
 * migration and the entities disagree, the application refuses to start.
 *
 * Usage: node tools/verify/migrations.mjs
 * Exit 0 and print "MIGRATIONS-PASS" when the schema is correct.
 */
import { execFileSync } from "node:child_process";
import { devEnv } from "./dev-env.mjs";

const HOST = process.env.SEO_BUILD_HOST || "tejes@pelican";
// LOCAL=1 runs everything on this machine. The continuous integration workflow uses it.
const LOCAL = process.env.SEO_LOCAL === "1" || process.env.SEO_LOCAL === "true";
const CONTAINER = "seo-v01-mysql";
const DB = process.env.SEO_DB_NAME || "emailsender_v01";
const DB_PORT = process.env.SEO_DB_PORT || "3307";
const ROOT_PASSWORD = devEnv("SEO_DB_ROOT_PASSWORD");
const DB_PASSWORD = devEnv("SEO_DB_PASSWORD");
const DB_USER = devEnv("SEO_DB_USERNAME", "seo_v01");
const APP = "seo-v01-migrations";
const PORT = 21500;

/**
 * Runs a command on the build host, or locally when LOCAL is set.
 *
 * @param {string} cmd the shell command
 * @returns {string} its output
 */
const sh = (cmd) =>
  LOCAL
    ? execFileSync("/bin/bash", ["-lc", cmd], { encoding: "utf8", maxBuffer: 32 * 1024 * 1024 })
    : execFileSync("ssh", [HOST, cmd], { encoding: "utf8", maxBuffer: 32 * 1024 * 1024 });
const say = (m) => console.log(`[migrations] ${m}`);

const TABLES = [
  "app_user",
  "app_role",
  "app_user_role",
  "client_group",
  "client",
  "email_template",
  "smtp_account",
  "gmail_account",
  "campaign",
  "delivery",
  "outbox",
  "extraction_run",
  "extraction_result",
  "inbox_message",
];

const COLUMNS = {
  app_user: ["id", "username", "password_hash", "status", "failed_login_count", "locked_until", "last_login_at"],
  app_user_role: ["user_id", "role_id", "granted_at"],
  client_group: ["id", "code", "name", "owner_user_id", "created_at", "updated_at", "deleted_at"],
  client: ["id", "name", "email", "web_link", "group_id", "owner_user_id", "deleted_at"],
  email_template: ["id", "name", "subject", "content_html", "status", "version", "approved_by", "approved_at"],
  smtp_account: ["id", "email", "protocol", "host", "port", "username", "auth_method", "enabled", "secret_ref"],
  gmail_account: ["id", "email", "oauth_client_id", "secret_ref", "refresh_token_ref", "scopes"],
  campaign: ["id", "from_account_id", "from_account_kind", "target_type", "template_id", "recipient_count", "success_count", "failure_count", "status", "error_detail", "started_at", "finished_at", "deleted_at"],
  delivery: ["id", "campaign_id", "recipient_email", "template_id", "status", "provider_message_id", "error_detail", "sent_at"],
  outbox: ["id", "from_account_id", "to_email", "draft_id", "subject", "body_html", "scheduled_at", "status", "attempt_count", "error_message", "deleted_at"],
  extraction_run: ["id", "status", "domain_count", "source_file_name", "started_at", "finished_at", "error_detail", "deleted_at"],
  extraction_result: ["id", "run_id", "domain", "email", "email_hash", "source_row"],
  inbox_message: ["id", "account_id", "provider_message_id", "from_address", "subject", "body_html", "received_at", "is_read"],
};

/**
 * Runs a query against the development database.
 *
 * The database may be a container on the build host, a container on this machine, or a
 * plain server such as the one the continuous integration workflow starts. The first
 * strategy that works is used.
 *
 * @param {string} query the SQL
 * @returns {string[]} the rows, one per line
 */
const sql = (query) => {
  const strategies = [
    `docker exec ${CONTAINER} mysql -uroot -p'${ROOT_PASSWORD}' -N -B -e "${query}" 2>/dev/null`,
    `mysql -h127.0.0.1 -P${DB_PORT} -uroot -p'${ROOT_PASSWORD}' -N -B -e "${query}" 2>/dev/null`,
  ];
  for (const command of strategies) {
    try {
      const out = sh(command);
      if (out !== undefined) {
        return out
          .trim()
          .split("\n")
          .map((l) => l.trim())
          .filter(Boolean);
      }
    } catch {
      // try the next strategy
    }
  }
  throw new Error("the development database is not reachable by any known strategy");
};

if (!ROOT_PASSWORD || !DB_PASSWORD) {
  console.error("MIGRATIONS-FAIL: the development credentials are not available.");
  console.error("Create .env.development at the repository root with SEO_DB_PASSWORD and SEO_DB_ROOT_PASSWORD.");
  process.exit(1);
}

try {
  // ---------------------------------------------------------------- clean slate
  say("dropping the database so the migrations run from nothing");
  // The database name is interpolated into the SQL. It is checked against a strict
  // pattern first, so it can never carry a shell or SQL metacharacter.
  if (!/^[a-z][a-z0-9_]{1,40}$/.test(DB)) {
    throw new Error(`the database name is not a plain identifier: ${DB}`);
  }
  sql(
    `DROP DATABASE IF EXISTS ${DB}; CREATE DATABASE ${DB} CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;`
  );

  // ---------------------------------------------------------------- boot
  say("building the image");
  sh(`cd ${LOCAL ? "." : "~/seo-rebuild/app"} && docker build -q -t seo-v01:migrations . 2>&1 | tail -1`);
  sh(`docker rm -f ${APP} >/dev/null 2>&1 || true`);
  say("booting the application so Flyway migrates and Hibernate validates the schema");
  sh(
    `docker run -d --name ${APP} --network host ` +
      `-e DB_URL="jdbc:mysql://127.0.0.1:3307/${DB}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" ` +
      `-e DB_USERNAME=${DB_USER} -e DB_PASSWORD='${DB_PASSWORD}' -e SERVER_PORT=${PORT} seo-v01:migrations >/dev/null`
  );

  let up = false;
  for (let i = 0; i < 60; i++) {
    await new Promise((r) => setTimeout(r, 2000));
    const code = sh(`curl -s -o /dev/null -w '%{http_code}' --max-time 5 http://127.0.0.1:${PORT}/login || true`).trim();
    if (code === "200") {
      up = true;
      break;
    }
  }
  if (!up) {
    const log = sh(`docker logs --tail 40 ${APP} 2>&1 || true`);
    throw new Error(`the application did not start against the clean schema\n${log}`);
  }
  say("the application started, so Flyway applied and Hibernate validated the schema");

  // ---------------------------------------------------------------- the tables
  const present = sql(
    `SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='${DB}' AND TABLE_TYPE='BASE TABLE';`
  );
  const missing = TABLES.filter((t) => !present.includes(t));
  if (missing.length) throw new Error(`missing tables: ${missing.join(", ")}`);
  say(`${TABLES.length} tables present`);

  // ---------------------------------------------------------------- the columns
  for (const [table, columns] of Object.entries(COLUMNS)) {
    const actual = sql(
      `SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='${DB}' AND TABLE_NAME='${table}';`
    );
    const absent = columns.filter((c) => !actual.includes(c));
    if (absent.length) throw new Error(`${table} is missing the column(s): ${absent.join(", ")}`);
  }
  say("every expected column is present");

  // ---------------------------------------------------------------- the charset
  const wrongCharset = sql(
    `SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='${DB}' AND TABLE_COLLATION NOT LIKE 'utf8mb4%';`
  );
  if (wrongCharset.length) {
    throw new Error(`these tables are not utf8mb4: ${wrongCharset.join(", ")}`);
  }
  say("every table is utf8mb4");

  // ---------------------------------------------------------------- the constraints
  const fkCount = sql(
    `SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='${DB}' AND CONSTRAINT_TYPE='FOREIGN KEY';`
  )[0];
  if (Number(fkCount) < 10) {
    throw new Error(`expected at least 10 foreign keys, found ${fkCount}`);
  }
  say(`${fkCount} foreign keys in place`);

  const uniqueIndexes = sql(
    `SELECT COUNT(DISTINCT INDEX_NAME) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='${DB}' AND NON_UNIQUE=0;`
  )[0];
  if (Number(uniqueIndexes) < 8) {
    throw new Error(`expected at least 8 unique indexes, found ${uniqueIndexes}`);
  }
  say(`${uniqueIndexes} unique indexes in place`);

  // ---------------------------------------------------------------- no secret column
  const secretColumns = sql(
    `SELECT TABLE_NAME, COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='${DB}' ` +
      `AND COLUMN_NAME IN ('password','secret','refreshToken','accessToken','clientSecret','refresh_token','access_token');`
  );
  if (secretColumns.length) {
    throw new Error(`the schema stores a credential in a column: ${secretColumns.join(", ")}`);
  }
  say("no column holds a credential");

  sh(`docker rm -f ${APP} >/dev/null 2>&1 || true`);
  console.log("MIGRATIONS-PASS");
  process.exit(0);
} catch (err) {
  console.error(`MIGRATIONS-FAIL: ${err.message}`);
  try {
    sh(`docker rm -f ${APP} >/dev/null 2>&1 || true`);
  } catch {
    /* nothing more to report */
  }
  process.exit(1);
}
