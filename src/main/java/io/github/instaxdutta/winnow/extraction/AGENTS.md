# extraction/

## Purpose

Turning a list of domains into a list of contact email addresses, plus the Amazon scraper
client that talks to the Puppeteer sidecar.

## Ownership

- `DomainCrawler` - fetches a domain and extracts addresses from the page.
- `HttpDomainCrawler` - the implementation of `DomainCrawler`.
- `UrlAllowList` - decides which hosts may be fetched.
- `ScraperProperties` - `seo.scraper.*`: the sidecar base URL and the host allow-list.
- `AmazonScraperClient` - talks to the Puppeteer sidecar.
- `ScrapedProduct` - one product the sidecar returned.
- `ExtractionWorker` - the bounded fan-out that crawls the domains of a run.
- `ExtractionSink` - where the addresses and the final status of a run go.

## Local Contracts

- Every outbound URL is validated by `UrlAllowList` before the request. A spreadsheet cell
  must never become an arbitrary request. Fetching `https://` plus a cell value with no
  allow-list is a server-side request forgery hole.
- Public addresses only. `127.0.0.0/8`, `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`,
  `169.254.0.0/16`, `0.0.0.0/8`, `::1`, `fc00::/7`, `fe80::/10` and the IPv4-mapped
  forms of those are rejected, together with multicast.
- The host is resolved and **every** address it resolves to is checked, so a public name
  that points at `127.0.0.1` is rejected.
- Only `https` and `http` are allowed. Redirects are followed at most
  `seo.extraction.max-redirects` times and each hop is re-validated against the allow-list.
- Every fetch has an explicit connect and read timeout, and a response-size cap.
- The crawler reads only the response body, never executes the page. No headless browser is
  used for domain crawling.
- Extracted addresses are validated against a conservative pattern and against the domain
  that was crawled. An address is stored once, deduplicated by a hash of the normalised
  address, and the unique constraint is enforced by the database.
- The per-run dedupe set is scoped to the run, not to the JVM. A process-wide map that is
  never cleared makes a re-run of the same domain return nothing.
- A run's fan-out is bounded by a fixed pool and a bounded queue. Work is submitted in
  chunks, not one task per row.
- Results are written in batches. Never one insert per address.
- The scraper client only ever requests the configured base URL. The keyword travels in a
  query parameter, never in the host or the path.

## Work Guidance

- A failed domain is recorded as a failed domain and does not fail the run.
- The run records a status that always terminates in `SUCCESS`, `PARTIAL` or `FAILED`. Never
  `IN PROGRESS` left behind by an exception.
- The Amazon scraper client talks to a sidecar over loopback with a request timeout. A
  sidecar bound to every interface with a wildcard CORS header and no authentication is an
  open proxy.
- The extraction package never writes to the database. `ExtractionWorker` calls an
  `ExtractionSink`, and the service or the job supplies it.

## Public API

| Type | Methods |
|---|---|
| `DomainCrawler` | `findEmails(String domain)` |
| `UrlAllowList` | `check(URL)` throws `ValidationException`, `isAllowed(URL)` |
| `HttpDomainCrawler` | `findEmails(String domain)` |
| `AmazonScraperClient` | `scrape(String keyword, int page)` |
| `ExtractionWorker` | `run(Long runId, List<DomainEntry> domains, ExtractionSink sink)` |
| `ExtractionSink` | `storeAddresses(List<FoundAddress>)`, `complete(ExtractionStatus, String)` |

## Verification

- `./mvnw test` - the extraction tests cover the allow-list rejections, the redirect
  re-validation, the deduplication, the batching and the failure isolation.

## Child DOX Index

This package has no child DOX files.
