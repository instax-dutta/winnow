"use strict";

/**
 * verify.js - proves the contract of the scraper sidecar without a browser.
 *
 * A stub replaces the page reader, so the whole HTTP surface can be checked on a machine
 * that has no Chromium: the shape of the answer, the refusal of a bad keyword, the empty
 * answer of a failed scrape, the loopback bind, and the rule that a keyword is a search
 * term and never a URL.
 *
 * Usage: node verify.js
 * Exit 0 and print "VERIFY-PASS" when every check holds, exit 1 with the findings
 * otherwise.
 */

const assert = require("node:assert/strict");
const { spawn } = require("node:child_process");
const fs = require("node:fs");
const net = require("node:net");
const os = require("node:os");
const path = require("node:path");

const { searchUrl } = require("./scrape");

const SERVER = path.join(__dirname, "server.js");

/** The stub stands in for the browser. It records what it was asked for. */
const STUB_SOURCE = `"use strict";
const fs = require("node:fs");
module.exports = async function scrape({ keyword, page }) {
  fs.appendFileSync(process.env.STUB_RECORD, JSON.stringify({ keyword: keyword, page: page }) + "\\n");
  if (keyword === "FAIL") throw new Error("the stub was told to fail");
  return [{
    title: "Test product",
    url: "https://www.amazon.in/dp/B0TEST0000",
    image: "https://m.media-amazon.com/images/I/test.jpg",
    price: "\\u20b9499",
    soldBy: "Sold by Example Retail",
    fulfilledBy: "Amazon"
  }];
};
`;

const findings = [];
const children = [];

function check(name, run) {
  return Promise.resolve()
    .then(run)
    .then(() => {
      process.stdout.write(`ok   ${name}\n`);
    })
    .catch((error) => {
      findings.push(`${name}: ${error.message}`);
      process.stdout.write(`FAIL ${name}: ${error.message}\n`);
    });
}

/**
 * Starts the service and waits for it to listen.
 *
 * @param {object} env the environment for the child
 * @param {object} options options
 * @param {string[]} options.omit environment names to leave out
 * @returns {Promise<object>} the port and the process
 */
function startServer(env, options = {}) {
  const childEnv = Object.assign({}, process.env, { PORT: "0" }, env);
  for (const name of options.omit || []) delete childEnv[name];
  return new Promise((resolve, reject) => {
    const proc = spawn(process.execPath, [SERVER], { env: childEnv, stdio: ["ignore", "pipe", "pipe"] });
    children.push(proc);
    proc.stderr.resume();
    let buffer = "";
    const timer = setTimeout(() => {
      proc.kill("SIGKILL");
      reject(new Error("the service did not start"));
    }, 20000);
    proc.stdout.on("data", (chunk) => {
      buffer += chunk.toString();
      for (const line of buffer.split("\n")) {
        if (!line.trim()) continue;
        let entry;
        try {
          entry = JSON.parse(line);
        } catch (error) {
          continue;
        }
        if (entry.message === "the scraper sidecar is listening") {
          clearTimeout(timer);
          resolve({ proc, port: entry.port });
          return;
        }
      }
    });
    proc.on("error", (error) => {
      clearTimeout(timer);
      reject(error);
    });
  });
}

/**
 * Whether a TCP connection to an address can be made.
 *
 * @param {string} host the address
 * @param {number} port the port
 * @returns {Promise<boolean>} whether the connection succeeded
 */
function canConnect(host, port) {
  return new Promise((resolve) => {
    const socket = net.connect({ host, port });
    const finish = (result) => {
      socket.destroy();
      resolve(result);
    };
    socket.setTimeout(2000, () => finish(false));
    socket.on("connect", () => finish(true));
    socket.on("error", () => finish(false));
  });
}

/**
 * The addresses of this machine that are not loopback.
 *
 * @returns {string[]} the addresses
 */
function externalAddresses() {
  const addresses = [];
  for (const entries of Object.values(os.networkInterfaces())) {
    for (const entry of entries || []) {
      if (entry.family !== "IPv4" && entry.family !== 4) continue;
      if (entry.internal) continue;
      addresses.push(entry.address);
    }
  }
  return addresses;
}

async function main() {
  const work = fs.mkdtempSync(path.join(os.tmpdir(), "winnow-scraper-"));
  const stubPath = path.join(work, "stub.js");
  const recordPath = path.join(work, "record.jsonl");
  fs.writeFileSync(stubPath, STUB_SOURCE);

  try {
    const service = await startServer({
      HOST: "127.0.0.1",
      SCRAPE_STUB: stubPath,
      STUB_RECORD: recordPath
    });
    const base = `http://127.0.0.1:${service.port}`;

    await check("the health check answers ok and starts no browser", async () => {
      const response = await fetch(`${base}/health`);
      assert.equal(response.status, 200);
      assert.match(response.headers.get("content-type") || "", /application\/json/);
      assert.deepEqual(await response.json(), { status: "ok" });
    });

    await check("the answer carries no CORS header", async () => {
      const response = await fetch(`${base}/scrape?keyword=test&page=1`);
      assert.equal(response.headers.get("access-control-allow-origin"), null);
    });

    await check("a scrape answers with the contract shape", async () => {
      const response = await fetch(`${base}/scrape?keyword=test&page=1`);
      assert.equal(response.status, 200);
      assert.match(response.headers.get("content-type") || "", /application\/json/);
      assert.deepEqual(await response.json(), {
        keyword: "test",
        page: 1,
        productCount: 1,
        products: [
          {
            title: "Test product",
            url: "https://www.amazon.in/dp/B0TEST0000",
            image: "https://m.media-amazon.com/images/I/test.jpg",
            price: "₹499",
            soldBy: "Sold by Example Retail",
            fulfilledBy: "Amazon"
          }
        ]
      });
    });

    await check("a missing keyword is refused", async () => {
      const response = await fetch(`${base}/scrape?page=1`);
      assert.equal(response.status, 400);
      const body = await response.json();
      assert.equal(typeof body.error, "string");
      assert.equal(body.products, undefined);
    });

    await check("an empty keyword is refused", async () => {
      const response = await fetch(`${base}/scrape?keyword=%20%20&page=1`);
      assert.equal(response.status, 400);
    });

    await check("a keyword with a control character is refused", async () => {
      const response = await fetch(`${base}/scrape?keyword=usb%0Ac&page=1`);
      assert.equal(response.status, 400);
    });

    await check("a failed scrape answers with an empty list", async () => {
      const response = await fetch(`${base}/scrape?keyword=FAIL&page=1`);
      assert.equal(response.status, 200);
      const body = await response.json();
      assert.deepEqual(body.products, []);
      assert.equal(body.productCount, 0);
    });

    await check("the keyword reaches the reader as a search term", async () => {
      const keyword = "https://evil.example/";
      const response = await fetch(`${base}/scrape?keyword=${encodeURIComponent(keyword)}&page=1`);
      assert.equal(response.status, 200);
      const recorded = fs
        .readFileSync(recordPath, "utf8")
        .split("\n")
        .filter(Boolean)
        .map((line) => JSON.parse(line));
      const last = recorded[recorded.length - 1];
      assert.equal(last.keyword, keyword);
      assert.equal(last.page, 1);
    });

    await check("the search URL keeps the fixed origin", () => {
      assert.equal(
        searchUrl("https://www.amazon.in", "https://evil.example/", 1),
        "https://www.amazon.in/s?k=https%3A%2F%2Fevil.example%2F&page=1"
      );
      assert.equal(searchUrl("https://www.amazon.in", "usb c hub", 3), "https://www.amazon.in/s?k=usb+c+hub&page=3");
    });

    await check("the service listens on the loopback interface only", async () => {
      const local = await startServer(
        { SCRAPE_STUB: stubPath, STUB_RECORD: recordPath },
        { omit: ["HOST"] }
      );
      try {
        assert.equal(await canConnect("127.0.0.1", local.port), true, "loopback must answer");
        const addresses = externalAddresses();
        if (addresses.length === 0) {
          process.stdout.write("     no non-loopback address on this machine, nothing else to check\n");
          return;
        }
        for (const address of addresses) {
          assert.equal(
            await canConnect(address, local.port),
            false,
            `the service must not answer on ${address}`
          );
        }
      } finally {
        local.proc.kill("SIGTERM");
      }
    });

    await check("an explicit HOST opens the interface on request", async () => {
      const addresses = externalAddresses();
      if (addresses.length === 0) {
        process.stdout.write("     no non-loopback address on this machine, nothing to open\n");
        return;
      }
      const wide = await startServer(
        { HOST: addresses[0], SCRAPE_STUB: stubPath, STUB_RECORD: recordPath },
        { omit: [] }
      );
      try {
        assert.equal(await canConnect(addresses[0], wide.port), true, "the chosen interface must answer");
      } finally {
        wide.proc.kill("SIGTERM");
      }
    });
  } finally {
    for (const proc of children) {
      try {
        proc.kill("SIGTERM");
      } catch (error) {
        // The process is already gone.
      }
    }
    fs.rmSync(work, { recursive: true, force: true });
  }

  if (findings.length > 0) {
    process.stderr.write(`VERIFY-FAIL: ${findings.length} check(s) failed\n`);
    for (const finding of findings) process.stderr.write(`  ${finding}\n`);
    process.exit(1);
  }
  process.stdout.write("VERIFY-PASS\n");
}

main().catch((error) => {
  process.stderr.write(`VERIFY-FAIL: ${error.stack}\n`);
  process.exit(1);
});
