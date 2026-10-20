# scraper/

## Purpose

The Amazon search sidecar. It is a small Node service that drives a real browser, reads
an Amazon search results page, and answers with the products it found.

It exists because the application needs a rendered page, and the application is Java. The
separation is deliberate: the browser is a heavy, awkward dependency that belongs in its own
container, and the Java side only ever speaks JSON to it.

## Ownership

| File | Holds |
|---|---|
| `server.js` | The HTTP service: routing, the browser lifecycle, the timeouts, the logging |
| `scrape.js` | The Amazon page reader and the search URL builder |
| `config.js` | Configuration read from the environment, and the keyword rules |
| `verify.js` | The contract proof. Runs without a browser |
| `Dockerfile` | The build and runtime stages |
| `package.json` | The dependencies, pinned, and the scripts |

## Local Contracts

- **The caller sends a search term, never a URL.** The service builds the Amazon URL itself
  from the keyword and the page number. A caller must never be able to choose the address
  the server visits.
- **Bind to loopback.** `HOST` defaults to `127.0.0.1`. `0.0.0.0` is reachable only when an
  operator sets it deliberately, and the service logs a warning when it is not loopback.
- **No CORS header.** The service is called by the application over the server's own
  network, never by a browser. There is no `Access-Control-Allow-Origin` anywhere.
- **Every input is bounded.** The keyword is at most 200 characters and carries no control
  characters. The page is between 1 and 20. At most 200 products come back.
- **Every wait is bounded.** A navigation timeout and a total request timeout, both
  configured.
- **One browser, reused.** It is launched at start-up, reused for every request, recreated
  if it dies, and closed on shutdown. A browser per request is slow and leaks processes.
- **A failed scrape is not a server error.** The service answers `200` with an empty
  `products` array. A bad request answers `400`.
- **One browser page is read at a time per scrape.** At most two scrapes run at once.
- **No secret.** The service needs no credential of its own.
- **CommonJS, Node 20 or later.** No build step, no transpiler.

## Work Guidance

- The DOM changes regularly. Read each field defensively, so that one field disappearing
  does not lose the whole product.
- The `SCRAPE_STUB` environment variable points the service at a module that exports a stub
  reader. It exists so the contract can be proved without a browser, and it is how the
  verification script works.
- The installer in `puppeteer` 24.x can leave a browser that will not start, because
  `extract-zip` 2.0.1 stops mid-archive. The Dockerfile therefore fetches and unpacks the
  browser itself and fails the build if the binary is not there. Do not go back to the
  ordinary install without checking that.

## Verification

```bash
cd scraper && npm test
```

It prints `VERIFY-PASS`. It proves the contract, the four security requirements and the
bounds, and it needs no browser and no network. The build stage of the Dockerfile is the
only thing that needs a browser.

## Child DOX Index

This directory has no child DOX files.
