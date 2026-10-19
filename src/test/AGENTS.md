# src/test/

## Purpose

The test suite. It must be hermetic: no network, no remote MySQL, no live mailbox, no
headless browser. The suite carries the three gates that belong to the test tree: the whole
suite passes (G2), one tenant cannot read or write another tenant's data (G7), and every
mutating endpoint refuses a request without a CSRF token (G9).

## Ownership

| Path | Holds |
|---|---|
| `java/com/pg/seo/<package>/` | Unit tests for the matching production package |
| `java/com/pg/seo/integration/` | Spring context tests against H2 with Flyway applied |
| `resources/application-test.yml` | The test profile. It shadows the one in `src/main/resources` |

The files in the tree:

| File | Covers |
|---|---|
| `integration/TenantIsolationIT.java` | Every repository method that returns tenant owned data, and every service that mutates it. G7 |
| `integration/CsrfProtectionIT.java` | Every mutating endpoint of the web layer, with and without a CSRF token. G9 |
| `integration/ApplicationContextIT.java` | The context loads, the beans are unambiguous, Flyway applied the migration, the mapping matches the schema |
| `integration/RepositoryQueryIT.java` | The migration on a clean schema, the soft delete predicate, the unique index on `(run_id, email_hash)` |
| `extraction/UrlAllowListTest.java` | The SSRF control: the private ranges, the scheme, the host suffix list |
| `extraction/HttpDomainCrawlerTest.java` | The crawler never reaches an internal address and only keeps the addresses of the domain it crawled |
| `mail/MailTemplateRendererTest.java` | Every substituted value is HTML escaped |
| `config/FileSecretStoreTest.java` | Store, resolve, delete, the owner only file, the name that discloses nothing |

## Local Contracts

- The test profile uses H2 in MySQL-compatibility mode with Flyway on. The same migrations
  that run in production run here.
- **Every test that loads the Spring context carries `@ActiveProfiles("test")`.** Without
  it the context uses the production datasource from `src/main/resources/application.yml`
  and tries to reach a real MySQL. That is the default configuration, not a fallback, and
  it is the fastest way to write a test that touches a real database by accident.
- The test profile in `src/test/resources` shadows the one in `src/main/resources`. It
  carries everything that file carries and holds the scheduled jobs back, so a job never
  runs while a test runs.
- No test reaches the network. A provider is mocked or replaced with a local fake. The one
  socket a test may open is a local `com.sun.net.httpserver` on the loopback interface.
- No test reads a secret from the environment. Test secrets are inline test fixtures.
- A test password is a fixture, not a secret.
- A test name states the behaviour: `tenantCannotReadAnotherTenantsClient`, not `testRead`.
- Every repository method that returns tenant-owned data has a test that proves the tenant
  filter, because that was the original's largest defect class.
- Every endpoint has a MockMvc test asserting the status code, the envelope shape and the
  tenant filter.
- A fixture is written before the test reads anything, so a fixture the schema cannot hold
  is a failure of the test and not a silent empty result.
- The suite never weakens an assertion to make the build green. A defect in the production
  code is reported, not accommodated.

## Work Guidance

- Prefer a test that fails for one reason. A test asserting three unrelated things fails
  unreadably.
- Use `@Sql` or the Flyway-applied schema for fixtures. No raw inserts scattered across tests.
- A flaky test is a bug in the test, not a reason to retry it.
- The integration tests are named `*IT`. Surefire does not run them by default, so the
  gate for the whole suite needs `-Dtest='*Test,*IT'`, or the surefire configuration has
  to name `**/*IT.java`.

## Verification

- `./mvnw verify` is the gate.
- `./mvnw test -Dtest=ClassName#method` while iterating.
- `./mvnw test -Dtest='TenantIsolationIT'` for G7, `./mvnw test -Dtest='CsrfProtectionIT'`
  for G9.

## Child DOX Index

This directory has no child DOX files.
