#!/usr/bin/env node
/**
 * dev-env.mjs - load the development credentials.
 *
 * The credentials for the local development database are NOT committed. They live in
 * `.env.development` at the repository root, which is git-ignored. A real environment
 * variable always wins over the file, so a deployment never depends on it.
 *
 * Usage: import { devEnv } from "./dev-env.mjs";
 */
import { readFileSync, existsSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..", "..");
const FILE = join(ROOT, ".env.development");

const fromFile = new Map();
if (existsSync(FILE)) {
  for (const line of readFileSync(FILE, "utf8").split(/\r?\n/)) {
    const t = line.trim();
    if (!t || t.startsWith("#")) continue;
    const i = t.indexOf("=");
    if (i < 0) continue;
    fromFile.set(t.slice(0, i).trim(), t.slice(i + 1).trim());
  }
}

/**
 * The development environment. The process environment wins, then the file, then nothing.
 *
 * @param {string} name the variable
 * @param {string} [fallback] used when neither source has it
 * @returns {string} the value
 */
export const devEnv = (name, fallback = "") => {
  const fromProcess = process.env[name];
  if (fromProcess !== undefined && fromProcess !== "") return fromProcess;
  return fromFile.get(name) ?? fallback;
};

/**
 * Whether the development credentials are available at all.
 */
export const hasDevEnv = () => fromFile.has("SEO_DB_PASSWORD") || !!process.env.SEO_DB_PASSWORD;
