# src/test/

## Purpose

The test suite. It must be hermetic: no network, no remote MySQL, no live mailbox, no
headless browser.

## Ownership

| Path | Holds |
|---|---|
| `java/com/pg/seo/<package>/` | Unit tests for the matching production package |
| `java/com/pg/seo/integration/` | Spring context tests against H2 with Flyway applied |
| `resources/application-test.yml` | The test profile |

## Local Contracts

- The test profile uses H2 in MySQL-compatibility mode with Flyway on. The same migrations
  that run in production run here.
- No test reaches the network. A provider is mocked or replaced with a local fake.
- No test reads a secret from the environment. Test secrets are inline test fixtures.
- A test name states the behaviour: `tenantCannotReadAnotherTenantsClient`, not `testRead`.
- Every repository method that returns tenant-owned data has a test that proves the tenant
  filter, because that was the original's largest defect class.
- Every endpoint has a MockMvc test asserting the status code, the envelope shape and the
  tenant filter.

## Work Guidance

- Prefer a test that fails for one reason. A test asserting three unrelated things fails
  unreadably.
- Use `@Sql` or the Flyway-applied schema for fixtures. No raw inserts scattered across tests.
- A flaky test is a bug in the test, not a reason to retry it.

## Verification

- `./mvnw verify` is the gate.
- `./mvnw test -Dtest=ClassName#method` while iterating.

## Child DOX Index

This directory has no child DOX files.
