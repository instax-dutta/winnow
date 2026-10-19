#!/usr/bin/env node
/**
 * parity.mjs - check that the migration document accounts for every endpoint and every
 * table of the original application.
 *
 * The original endpoint and table lists are extracted from the reverse engineering record,
 * not from a hand-copied list, so the check cannot drift away from the evidence.
 *
 * Usage: node tools/verify/parity.mjs
 * Exit 0 and print "PARITY-PASS" when nothing is unaccounted for.
 */
import { readFileSync, existsSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..", "..");

const ENDPOINTS_DOC = "docs/reverse-engineering/02-controllers-and-security.md";
const TABLES_DOC = "docs/reverse-engineering/03-entities-and-templates.md";
const MIGRATION_DOC = "docs/MIGRATION-FROM-0.0.1.md";

const read = (p) => {
  const full = join(ROOT, p);
  if (!existsSync(full)) throw new Error(`${p} does not exist`);
  return readFileSync(full, "utf8");
};

const migration = read(MIGRATION_DOC).toLowerCase();
const findings = [];

/**
 * Extracts the original endpoints from the endpoint tables of the analysis.
 *
 * The tables have the shape `| GET | `/path` | ... |`, so the path is the second cell.
 */
const endpointDoc = read(ENDPOINTS_DOC);
const endpoints = new Set();
for (const line of endpointDoc.split("\n")) {
  const m = /^\|\s*(GET|POST|PUT|DELETE|PATCH)\s*\|\s*`([^`]+)`/.exec(line);
  if (!m) continue;
  const path = m[2].trim();
  if (!path.startsWith("/")) continue;
  endpoints.add(path);
}

/** The original tables, from the analysis of the entities and the schema. */
const tablesDoc = read(TABLES_DOC);
const tables = new Set();
// A SQL type is not a table. These are the words the analysis uses next to a column
// type, and they must never be mistaken for a table name.
const NOT_A_TABLE = new Set([
  "bigint", "varchar", "int", "integer", "smallint", "tinyint", "boolean", "decimal",
  "numeric", "double", "float", "timestamp", "datetime", "text", "mediumtext", "longtext",
  "tinytext", "binary", "varbinary", "blob", "json", "enum", "date", "time", "key",
  "primary", "foreign", "unique", "index", "null", "default", "auto_increment", "unsigned",
]);
for (const m of tablesDoc.matchAll(/`([a-z][a-z0-9_]{3,})`\s*(?:table|PK|PRIMARY KEY|->)/gi)) {
  const name = m[1].toLowerCase();
  if (name.startsWith("app_") || name.startsWith("uk_") || name.startsWith("idx_")) continue;
  if (NOT_A_TABLE.has(name)) continue;
  tables.add(name);
}
// The tables that the analysis names explicitly.
for (const name of [
  "login",
  "login_role_security",
  "role_security",
  "client_group",
  "client_master",
  "email_template",
  "email_master",
  "gmail_master",
  "email_send_job",
  "email_send_status",
  "email_queue",
  "email_extractor",
  "email_extractor_data",
  "email_inbox",
]) {
  tables.add(name);
}

for (const path of endpoints) {
  if (!migration.includes(path.toLowerCase())) {
    findings.push(`the original endpoint ${path} is not accounted for in ${MIGRATION_DOC}`);
  }
}
for (const table of tables) {
  if (!migration.includes(table)) {
    findings.push(`the original table ${table} is not accounted for in ${MIGRATION_DOC}`);
  }
}

// The rebuild's own tables have to be there too.
const schema = read("src/main/resources/db/migration/V1__baseline.sql");
for (const m of schema.matchAll(/CREATE TABLE\s+`?([a-z][a-z0-9_]+)`?/gi)) {
  const table = m[1].toLowerCase();
  if (!migration.includes(table)) {
    findings.push(`the new table ${table} is not documented in ${MIGRATION_DOC}`);
  }
}

if (findings.length) {
  console.error(`PARITY-FAIL: ${findings.length} unaccounted item(s)`);
  for (const f of findings) console.error(`  ${f}`);
  process.exit(1);
}
console.log(
  `PARITY-PASS: ${endpoints.size} original endpoints and ${tables.size} original tables are accounted for`
);
