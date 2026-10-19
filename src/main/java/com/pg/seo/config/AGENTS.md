# config/

## Purpose

Bean definitions, typed configuration properties and infrastructure wiring.

## Ownership

- `*Properties` - `@ConfigurationProperties` records, one per configuration group. Every
  property is validated at startup with `@Validated` and a bound, so a bad value fails the
  boot instead of the first request.
- `JpaConfig` - the datasource, entity manager factory and transaction manager.
- `AsyncConfig` - the task executors, each with a name, a bounded queue and explicit
  rejection handling.
- `OpenApiConfig` - the OpenAPI definition, when the API docs are enabled.

## Local Contracts

- No `application.yml` value is a secret. Secrets arrive from the environment or the secret
  store and are bound through a properties record, so they never appear in a properties file.
- Every executor is named, bounded and has a documented rejection policy. The original
  declared a `taskExecutor` method without `@Bean`, so the pool it described never existed
  and the work silently ran on the common pool.
- No bean is declared in two places. The original had two `BCryptPasswordEncoder` beans.
- Package scanning uses real package names, not the wildcard `com.pg.*` the original used.

## Work Guidance

- Adding a property: add it to the properties record with a default and a validation rule,
  add it to `application.yml` with the same default, and reference it through the record
  rather than through `@Value`.
- A configuration change that alters behaviour needs a line in `docs/ARCHITECTURE.md`.

## Verification

- `./mvnw test` - the context load test fails if any bean is ambiguous, missing or invalid.

## Child DOX Index

This package has no child DOX files.
