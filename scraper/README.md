# winnow scraper

The Amazon search sidecar of `winnow`. It renders one page of Amazon search results in a
real browser and answers with JSON. The application calls it over the server's own
network; nothing in a browser ever calls it.

The service is deliberately small and deliberately narrow:

- it listens on the loopback interface and nowhere else,
- it sends no CORS header,
- it takes a **keyword**, never a URL: the Amazon address is built here, from a fixed
  origin and the keyword, so a caller cannot make the browser visit a place it chooses,
- it bounds every piece of work it does: a keyword length, a page number, a product
  count, a navigation timeout and a total timeout,
- and a scrape that fails is an empty list, not an error. The caller treats "no results"
  and "could not read the page" the same way.

## The contract

**Request**

```
GET /scrape?keyword=<url-encoded keyword>&page=<1-based page number>
```

`page` is optional and defaults to 1. The keyword is a search term. A keyword that looks
like a URL is still a search term: it is percent-encoded into the query string of the
Amazon address and goes nowhere else.

**Answer**

```json
{
  "keyword": "usb c hub",
  "page": 1,
  "productCount": 42,
  "products": [
    {
      "title": "Product name",
      "url": "https://www.amazon.in/dp/B0XXXXXXX",
      "image": "https://m.media-amazon.com/images/I/....jpg",
      "price": "₹499",
      "soldBy": "Sold by Example Retail",
      "fulfilledBy": "Amazon"
    }
  ]
}
```

Every field of every product is optional. A product with only a title is a valid answer,
and a missing field is `null` rather than an empty string.

The search results page of `amazon.in` carries the title, the link, the image and the
price. It carries the seller only for some of the results, so `soldBy` and `fulfilledBy`
are often `null`.

**Health**

```
GET /health  ->  200 {"status":"ok"}
```

The health check never starts a browser.

**Errors**

| Situation | Answer |
|---|---|
| The keyword is missing, empty, longer than 200 characters or carries a control character | `400 {"error": "..."}` |
| The page number is not a number, or is above 20 | `400 {"error": "..."}` |
| The page could not be read, or the scrape timed out | `200` with an empty `products` array |
| Any other path | `404 {"error": "not found"}` |

The answer always carries `Content-Type: application/json`. There is no HTML, no stack
trace and no file path in a response.

## Running it

### With Docker

```bash
cd scraper
docker build -t winnow-scraper .
docker run --rm -p 127.0.0.1:3000:3000 winnow-scraper
```

The service binds to the loopback interface inside the container. To let another
container reach it, publish it and set the host explicitly:

```bash
docker run --rm -e HOST=0.0.0.0 -p 3000:3000 winnow-scraper
```

### Without Docker

```bash
cd scraper
npm install
npm start
```

`npm install` downloads the browser. The service listens on `127.0.0.1:3000` unless the
environment says otherwise.

If the download leaves a browser that cannot start, fetch it by hand. The version is the
one puppeteer pins, so the two cannot drift apart:

```bash
version=$(node -p "require('puppeteer').PUPPETEER_REVISIONS.chrome")
curl -fsSL -o /tmp/chrome.zip \
  "https://storage.googleapis.com/chrome-for-testing-public/${version}/linux64/chrome-linux64.zip"
mkdir -p "${PUPPETEER_CACHE_DIR:-$HOME/.cache/puppeteer}/chrome/linux-${version}"
unzip -q /tmp/chrome.zip -d "${PUPPETEER_CACHE_DIR:-$HOME/.cache/puppeteer}/chrome/linux-${version}"
```

On macOS the archive holds `chrome-mac-arm64` instead of `chrome-linux64`, and the
executable is inside `Google Chrome for Testing.app`. A browser that is already on the
machine is used as well: set `PUPPETEER_EXECUTABLE_PATH` to its binary.

## Configuration

Every setting is an environment variable. The default is the safe one.

| Variable | Default | What it does |
|---|---|---|
| `HOST` | `127.0.0.1` | The interface to listen on. A container needs `HOST=0.0.0.0`, and that is a deliberate choice. The service warns at start-up when it is not a loopback address. |
| `PORT` | `3000` | The port to listen on. `0` picks a free port. |
| `AMAZON_BASE_URL` | `https://www.amazon.in` | The origin the search address is built from, and the origin every product link is checked against. |
| `MAX_KEYWORD_LENGTH` | `200` | The longest keyword that is accepted. |
| `MAX_PAGE` | `20` | The highest page number that is accepted. Amazon itself stops at 20. |
| `MAX_PRODUCTS` | `200` | The most products one answer carries. |
| `NAVIGATION_TIMEOUT_MS` | `20000` | How long one page load may take. |
| `REQUEST_TIMEOUT_MS` | `45000` | How long the whole request may take, browser included. |
| `BROWSER_LAUNCH_TIMEOUT_MS` | `30000` | How long the browser may take to start. |
| `MAX_CONCURRENT_SCRAPES` | `2` | How many scrapes may run at the same time. |
| `USER_AGENT` | (the browser's own) | Pins the user agent. By default the agent of the browser that is running, without the word "Headless". |
| `LOG_LEVEL` | `info` | `error`, `warn`, `info` or `debug`. |
| `SCRAPE_STUB` | (none) | A path to a module that replaces the page reader. Used to test the contract without a browser. |

`PUPPETEER_EXECUTABLE_PATH` and `PUPPETEER_CACHE_DIR` are honoured by Puppeteer itself.
The first one points the service at a browser that is already on the machine.

## The log

One JSON object per line, on stdout.

```json
{"time":"2026-01-01T00:00:00.000Z","level":"info","message":"the scrape finished","page":1,"keywordLength":10,"productCount":42,"durationMs":1830}
{"time":"2026-01-01T00:00:00.000Z","level":"warn","message":"the request was refused","reason":"the keyword is required","keywordLength":0}
```

The keyword itself is never written to the log, and the HTML of a page is never written
to the log. A line carries the outcome, the duration and the product count.

## Debugging it

**The service answers but every scrape is empty.** Look for `"level":"warn"` with
`"message":"the scrape failed"` and the error name. The usual causes are a page layout
Amazon has changed, a CAPTCHA, or the browser refusing to start.

**The browser does not start.** The message carries the reason. On a machine without the
shared libraries Chromium needs, the browser cannot start at all. In the container they
are installed. Locally, the browser is the one `npm install` downloaded; set
`PUPPETEER_EXECUTABLE_PATH` to use another one.

**The layout changed.** Every field is read on its own, so one field that moved costs
that field and not the product. To see what the page holds, run the reader against a
saved page:

```js
const puppeteer = require("puppeteer-extra");
const { readResults } = require("./scrape");
const browser = await puppeteer.launch({ headless: true });
const page = await browser.newPage();
await page.goto("https://www.amazon.in/s?k=usb+c+hub", { waitUntil: "domcontentloaded" });
console.log(await page.evaluate(readResults, 50));
```

The selectors live in `readResults` in `scrape.js`, one list per field.

**The service is not reachable.** It binds to `127.0.0.1`. From another container it
needs `HOST=0.0.0.0`, and from another machine it needs that too, and a published port.

## Checking it

`npm test` proves the contract without a browser. A stub replaces the page reader, so the
whole HTTP surface is checked on a machine that has no Chromium:

```bash
cd scraper
npm run check   # node --check on every file
npm test        # the contract, the loopback bind and the keyword rule
```

The checks are:

- `/health` answers `200 {"status":"ok"}` and starts no browser,
- no answer carries a CORS header,
- `GET /scrape?keyword=test&page=1` answers with the shape above,
- a missing, an empty and a control character keyword answer `400`,
- a scrape that fails answers `200` with an empty `products` array,
- the service answers on `127.0.0.1` and on nothing else, unless `HOST` says otherwise,
- and a keyword that is a URL reaches the reader as a search term, while the search
  address keeps the fixed origin.

## The browser

One browser serves every request. It is started once, reused, and closed on shutdown. A
browser that dies is replaced on the next request. `puppeteer-extra` with the stealth
plugin is used so the browser does not announce itself, and the user agent is the one of
the browser that is running, with the word "Headless" taken out.

The container runs the browser with `--no-sandbox`, because a container has no user
namespaces for the Chromium sandbox to use. The service itself runs as an unprivileged
user.

### A note on the pinned version

`puppeteer` is pinned to 24.43.1, the last line that still supports Node 20. That line
The dependency tree is clean. `puppeteer` 25 pulls `@puppeteer/browsers` 3, which no longer
depends on `extract-zip` or `basic-ftp`, so the two advisories that affected the `puppeteer` 24
installer are gone rather than merely unreachable. `npm audit` reports nothing.
