"use strict";

/**
 * The Amazon scraper sidecar.
 *
 * The application calls this service over the server's own network. It asks for one page
 * of Amazon search results for a keyword, and gets JSON back. A failed scrape is an
 * empty list, not an error: the caller treats "no results" and "could not read the page"
 * the same way.
 *
 * The service binds to the loopback interface, sends no CORS header, and never takes a
 * URL from the caller. The keyword is a search term, and the Amazon URL is built here.
 */

const http = require("node:http");
const path = require("node:path");

const { loadConfig, validateKeyword } = require("./config");

const LEVELS = { error: 0, warn: 1, info: 2, debug: 3 };

/** Marks the request that ran out of time. */
const TIMED_OUT = Symbol("timed out");

/**
 * A small structured logger. One JSON object per line, on stdout.
 *
 * @param {string} level the lowest level that is written
 * @returns {object} the logger
 */
function createLogger(level) {
  const threshold = LEVELS[level] === undefined ? LEVELS.info : LEVELS[level];
  const write = (name, message, fields) => {
    if (LEVELS[name] > threshold) return;
    const line = Object.assign({ time: new Date().toISOString(), level: name, message }, fields || {});
    process.stdout.write(`${JSON.stringify(line)}\n`);
  };
  return {
    error: (message, fields) => write("error", message, fields),
    warn: (message, fields) => write("warn", message, fields),
    info: (message, fields) => write("info", message, fields),
    debug: (message, fields) => write("debug", message, fields)
  };
}

/**
 * Loads puppeteer and the stealth plugin, once, and only when a browser is needed. A
 * service running against a stub never loads either.
 *
 * @returns {object} the configured puppeteer
 */
function loadPuppeteer() {
  if (!loadPuppeteer.instance) {
    const puppeteer = require("puppeteer-extra");
    puppeteer.use(require("puppeteer-extra-plugin-stealth")());
    loadPuppeteer.instance = puppeteer;
  }
  return loadPuppeteer.instance;
}

/**
 * Owns the browser. One browser serves every request, and a new one is started when the
 * old one has gone.
 *
 * @param {object} config the configuration
 * @param {object} logger the logger
 * @returns {object} the browser manager
 */
function createBrowserManager(config, logger) {
  let browser = null;
  let starting = null;

  const launch = async () => {
    const puppeteer = loadPuppeteer();
    const instance = await puppeteer.launch({
      headless: true,
      args: [
        // The container has no user namespaces and a small /dev/shm, so the sandbox and
        // the shared memory file are both problems that are solved here, once.
        "--no-sandbox",
        "--disable-setuid-sandbox",
        "--disable-dev-shm-usage",
        "--disable-gpu",
        "--lang=en-IN"
      ],
      defaultViewport: { width: 1536, height: 864 },
      timeout: config.browserLaunchTimeoutMs
    });
    instance.on("disconnected", () => {
      if (browser === instance) browser = null;
      logger.warn("the browser disconnected");
    });
    return instance;
  };

  return {
    async get() {
      if (browser && browser.connected) return browser;
      if (!starting) {
        starting = launch()
          .then((instance) => {
            browser = instance;
            starting = null;
            return instance;
          })
          .catch((error) => {
            starting = null;
            throw error;
          });
      }
      return starting;
    },
    discard(instance) {
      if (browser === instance) browser = null;
    },
    async close() {
      const instance = browser;
      browser = null;
      starting = null;
      if (instance) await instance.close().catch(() => null);
    }
  };
}

/**
 * Limits how many scrapes run at the same time.
 *
 * @param {number} limit the most scrapes at once
 * @returns {function} a function that takes a task and returns a promise
 */
function createLimiter(limit) {
  let active = 0;
  const waiting = [];
  const start = () => {
    if (active >= limit || waiting.length === 0) return;
    active += 1;
    const task = waiting.shift();
    Promise.resolve()
      .then(task.run)
      .then(task.resolve, task.reject)
      .finally(() => {
        active -= 1;
        start();
      });
  };
  return (run) =>
    new Promise((resolve, reject) => {
      waiting.push({ run, resolve, reject });
      start();
    });
}

/**
 * Answers with JSON and nothing else. There is no CORS header here on purpose: the
 * caller is the application on the server's own network, not a browser.
 *
 * @param {object} res the response
 * @param {number} status the status code
 * @param {object} payload the body
 */
function sendJson(res, status, payload) {
  const body = JSON.stringify(payload);
  res.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    "Content-Length": Buffer.byteLength(body),
    "Cache-Control": "no-store"
  });
  res.end(body);
}

/**
 * Reads the page number from the request.
 *
 * @param {string|null} raw the raw value
 * @param {object} config the configuration
 * @returns {{ok: boolean, value?: number, error?: string}} the verdict
 */
function parsePage(raw, config) {
  if (raw === null || raw === undefined || String(raw).trim() === "") return { ok: true, value: 1 };
  const text = String(raw).trim();
  if (!/^\d{1,5}$/.test(text)) return { ok: false, error: "the page number is not a number" };
  const value = Number(text);
  if (value < 1) return { ok: false, error: "the page number starts at 1" };
  if (value > config.maxPage) return { ok: false, error: `the page number is at most ${config.maxPage}` };
  return { ok: true, value };
}

/**
 * Tells a dead browser from any other failure.
 *
 * @param {Error} error the failure
 * @returns {boolean} whether the browser has gone
 */
function browserIsGone(error) {
  const message = String(error && error.message ? error.message : error);
  return (
    error.name === "TargetCloseError" ||
    /Target closed|Protocol error|browser has disconnected|Connection closed|Session closed/i.test(message)
  );
}

/**
 * Runs one scrape, and starts a new browser if the old one has gone.
 *
 * @param {object} ctx the service context
 * @param {string} keyword the search term
 * @param {number} page the page number
 * @returns {Promise<Array<object>>} the products
 */
async function scrapeOnce(ctx, keyword, page) {
  if (ctx.config.scrapeStub) {
    return ctx.reader({ keyword, page, browser: null, config: ctx.config });
  }
  const browser = await ctx.browsers.get();
  try {
    return await ctx.reader({ keyword, page, browser, config: ctx.config });
  } catch (error) {
    if (!browserIsGone(error)) throw error;
    ctx.logger.warn("the browser was gone, a new one is started");
    ctx.browsers.discard(browser);
    const fresh = await ctx.browsers.get();
    return ctx.reader({ keyword, page, browser: fresh, config: ctx.config });
  }
}

/**
 * Runs one scrape under the total time limit.
 *
 * @param {object} ctx the service context
 * @param {string} keyword the search term
 * @param {number} page the page number
 * @returns {Promise<Array<object>>} the products, empty when the scrape failed
 */
async function runScrape(ctx, keyword, page) {
  const work = ctx.limiter(() => scrapeOnce(ctx, keyword, page));
  let timer = null;
  const guard = new Promise((resolve) => {
    timer = setTimeout(() => resolve(TIMED_OUT), ctx.config.requestTimeoutMs);
  });
  try {
    const result = await Promise.race([work, guard]);
    if (result === TIMED_OUT) {
      ctx.logger.warn("the scrape timed out", { page });
      return [];
    }
    return result;
  } catch (error) {
    ctx.logger.warn("the scrape failed", { error: error.name, page });
    return [];
  } finally {
    clearTimeout(timer);
  }
}

/**
 * Answers one request.
 *
 * @param {object} req the request
 * @param {object} res the response
 * @param {object} ctx the service context
 */
async function handle(req, res, ctx) {
  const url = new URL(req.url, "http://127.0.0.1");
  const route = url.pathname.replace(/\/+$/, "") || "/";

  if (req.method !== "GET" && req.method !== "HEAD") {
    return sendJson(res, 405, { error: "only GET is allowed" });
  }

  if (route === "/health") {
    // No browser is touched here. A health check must never start one.
    return sendJson(res, 200, { status: "ok" });
  }

  if (route !== "/scrape") {
    return sendJson(res, 404, { error: "not found" });
  }

  const started = Date.now();
  const raw = url.searchParams.get("keyword");
  const check = validateKeyword(raw, ctx.config);
  if (!check.ok) {
    // The keyword itself is never logged. It came from a caller and it may be anything.
    ctx.logger.warn("the request was refused", {
      reason: check.error,
      keywordLength: typeof raw === "string" ? raw.length : 0
    });
    return sendJson(res, 400, { error: check.error });
  }

  const page = parsePage(url.searchParams.get("page"), ctx.config);
  if (!page.ok) {
    ctx.logger.warn("the request was refused", { reason: page.error });
    return sendJson(res, 400, { error: page.error });
  }

  const products = await runScrape(ctx, check.keyword, page.value);
  ctx.logger.info("the scrape finished", {
    page: page.value,
    keywordLength: check.keyword.length,
    productCount: products.length,
    durationMs: Date.now() - started
  });
  return sendJson(res, 200, {
    keyword: check.keyword,
    page: page.value,
    productCount: products.length,
    products
  });
}

/**
 * Creates the HTTP service.
 *
 * @param {object} ctx the service context
 * @returns {object} the server
 */
function createServer(ctx) {
  return http.createServer((req, res) => {
    handle(req, res, ctx).catch((error) => {
      ctx.logger.error("the request could not be answered", { error: error.name });
      if (!res.headersSent) sendJson(res, 500, { error: "the request could not be answered" });
    });
  });
}

/**
 * Loads the page reader. A stub replaces the browser entirely, which is how the contract
 * is proved without a browser.
 *
 * @param {object} config the configuration
 * @returns {function} the reader
 */
function loadReader(config) {
  if (!config.scrapeStub) return require("./scrape").scrape;
  const stub = require(path.resolve(config.scrapeStub));
  const reader = typeof stub === "function" ? stub : stub.scrape;
  if (typeof reader !== "function") throw new Error("the scrape stub does not export a function");
  return reader;
}

/**
 * Starts the service.
 *
 * @returns {object} the running service
 */
function start() {
  const config = loadConfig();
  const logger = createLogger(config.logLevel);
  const reader = loadReader(config);
  const ctx = {
    config,
    logger,
    reader,
    browsers: createBrowserManager(config, logger),
    limiter: createLimiter(config.maxConcurrentScrapes)
  };

  const server = createServer(ctx);

  server.listen(config.port, config.host, () => {
    const address = server.address();
    logger.info("the scraper sidecar is listening", { host: config.host, port: address.port });
    if (!isLoopback(config.host)) {
      logger.warn("the service is listening on every interface, set HOST=127.0.0.1 to keep it local");
    }
  });

  if (!config.scrapeStub) {
    // One browser, started once and reused. A failure here is not fatal: the next
    // request starts it again.
    ctx.browsers
      .get()
      .then(() => logger.info("the browser is ready"))
      .catch((error) => logger.error("the browser could not be started", { error: String(error.message).slice(0, 300) }));
  }

  let stopping = false;
  const shutdown = (signal) => {
    if (stopping) return;
    stopping = true;
    logger.info("the service is stopping", { signal });
    server.close();
    ctx.browsers.close().finally(() => process.exit(0));
  };
  process.on("SIGINT", () => shutdown("SIGINT"));
  process.on("SIGTERM", () => shutdown("SIGTERM"));

  return { server, ctx };
}

/**
 * Whether a host name is a loopback address.
 *
 * @param {string} host the host name
 * @returns {boolean} whether it is loopback
 */
function isLoopback(host) {
  return host === "localhost" || host === "127.0.0.1" || host === "::1" || host.startsWith("127.");
}

if (require.main === module) {
  start();
}

module.exports = { start, createServer, createLogger, createLimiter, parsePage, sendJson };
