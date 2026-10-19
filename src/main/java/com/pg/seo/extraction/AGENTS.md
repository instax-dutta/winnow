# extraction/

## Purpose

Turning a list of domains into a list of contact email addresses, plus the Amazon scraper
client that the original shipped alongside it.

## Ownership

- `ExtractionService` - runs an extraction over an uploaded domain list.
- `DomainCrawler` - fetches a domain and extracts addresses from the page.
- `UrlAllowList` - decides which hosts may be fetched.
- `AmazonScraperClient` - talks to the Puppeteer sidecar.

## Local Contracts

- Every outbound URL is validated by `UrlAllowList` before the request. A spreadsheet cell
  must never become an arbitrary request. The original fetched `https://` plus a cell value
  with no allow-list, which is a server-side request forgery hole.
- Public addresses only. `127.0.0.0/8`, `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`,
  `169.254.0.0/16`, `::1` and the link-local and unique-local ranges are rejected.
- Only `https` and `http` are allowed. Redirects are followed at most 3 times and each hop is
  re-validated against the allow-list.
- Every fetch has an explicit connect and read timeout, and a response-size cap.
- The crawler reads only the response body, never executes the page. No headless browser is
  used for domain crawling.
- Extracted addresses are validated against a conservative pattern and against the domain
  that was crawled. An address is stored once, deduplicated by a hash of the normalised
  address, and the unique constraint is enforced by the database.
- The per-run dedupe set is scoped to the run, not to the JVM. The original kept a
  process-wide map that was never cleared, so a re-run of the same domain returned nothing.
- A run's fan-out is bounded by a fixed pool and a bounded queue. Work is submitted in
  chunks, not one task per row.
- Results are written in batches. Never one insert per address.

## Work Guidance

- A failed domain is recorded as a failed domain and does not fail the run.
- The run records a status that always terminates in `SUCCESS`, `PARTIAL` or `FAILED`. Never
  `IN PROGRESS` left behind by an exception.
- The Amazon scraper client talks to a sidecar over loopback with a request timeout. The
  original bound the sidecar to every interface with a wildcard CORS header and no
  authentication, which made it an open proxy.

## Verification

- `./mvnw test` - the extraction tests cover the allow-list rejections, the redirect
  re-validation, the deduplication, the batching and the failure isolation.

## Child DOX Index

This package has no child DOX files.
