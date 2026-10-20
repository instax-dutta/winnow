"use strict";

/**
 * Configuration for the Amazon scraper sidecar, read from the environment.
 *
 * Every limit that bounds the work the service does lives here, so the whole surface an
 * operator can influence is in one file. The defaults are the safe ones: the service
 * listens on the loopback interface, and it refuses a keyword that is empty, too long or
 * carries control characters.
 */

/** The values used when the environment says nothing. */
const DEFAULTS = Object.freeze({
  host: "127.0.0.1",
  port: 3000,
  amazonBaseUrl: "https://www.amazon.in",
  maxKeywordLength: 200,
  maxPage: 20,
  maxProducts: 200,
  navigationTimeoutMs: 20000,
  requestTimeoutMs: 45000,
  browserLaunchTimeoutMs: 30000,
  maxConcurrentScrapes: 2,
  userAgent: null,
  scrapeStub: null,
  logLevel: "info"
});

/** C0, DEL and C1. A keyword carrying one of these is not a search term. */
const CONTROL_CHARACTERS = /[\u0000-\u001F\u007F-\u009F]/;

function readString(env, name, fallback) {
  const raw = env[name];
  if (raw === undefined || raw === null) return fallback;
  const value = String(raw).trim();
  return value === "" ? fallback : value;
}

function readInt(env, name, fallback, min) {
  const raw = env[name];
  if (raw === undefined || raw === null || String(raw).trim() === "") return fallback;
  const value = Number(String(raw).trim());
  if (!Number.isInteger(value) || value < min) return fallback;
  return value;
}

/**
 * Reads the configuration.
 *
 * @param {object} env the environment, normally `process.env`
 * @returns {object} the configuration
 */
function loadConfig(env = process.env) {
  const level = readString(env, "LOG_LEVEL", DEFAULTS.logLevel).toLowerCase();
  return {
    host: readString(env, "HOST", DEFAULTS.host),
    port: readInt(env, "PORT", DEFAULTS.port, 0),
    amazonBaseUrl: readString(env, "AMAZON_BASE_URL", DEFAULTS.amazonBaseUrl).replace(/\/+$/, ""),
    maxKeywordLength: readInt(env, "MAX_KEYWORD_LENGTH", DEFAULTS.maxKeywordLength, 1),
    maxPage: readInt(env, "MAX_PAGE", DEFAULTS.maxPage, 1),
    maxProducts: readInt(env, "MAX_PRODUCTS", DEFAULTS.maxProducts, 1),
    navigationTimeoutMs: readInt(env, "NAVIGATION_TIMEOUT_MS", DEFAULTS.navigationTimeoutMs, 1000),
    requestTimeoutMs: readInt(env, "REQUEST_TIMEOUT_MS", DEFAULTS.requestTimeoutMs, 1000),
    browserLaunchTimeoutMs: readInt(env, "BROWSER_LAUNCH_TIMEOUT_MS", DEFAULTS.browserLaunchTimeoutMs, 1000),
    maxConcurrentScrapes: readInt(env, "MAX_CONCURRENT_SCRAPES", DEFAULTS.maxConcurrentScrapes, 1),
    userAgent: readString(env, "USER_AGENT", DEFAULTS.userAgent),
    scrapeStub: readString(env, "SCRAPE_STUB", DEFAULTS.scrapeStub),
    logLevel: ["error", "warn", "info", "debug"].includes(level) ? level : DEFAULTS.logLevel
  };
}

/**
 * Checks a keyword against the limits.
 *
 * The keyword is a search term. It is never a URL, so the only thing to check is whether
 * it is usable as a search term at all.
 *
 * @param {string} keyword the raw keyword from the request
 * @param {object} config the configuration
 * @returns {{ok: boolean, keyword?: string, error?: string}} the verdict
 */
function validateKeyword(keyword, config) {
  if (typeof keyword !== "string" || keyword.trim() === "") {
    return { ok: false, error: "the keyword is required" };
  }
  const value = keyword.trim();
  if (value.length > config.maxKeywordLength) {
    return { ok: false, error: `the keyword is longer than ${config.maxKeywordLength} characters` };
  }
  if (CONTROL_CHARACTERS.test(value)) {
    return { ok: false, error: "the keyword contains control characters" };
  }
  return { ok: true, keyword: value };
}

module.exports = { loadConfig, validateKeyword, DEFAULTS, CONTROL_CHARACTERS };
