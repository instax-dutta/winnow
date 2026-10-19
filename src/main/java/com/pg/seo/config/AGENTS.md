# config/

## Purpose

Bean definitions, typed configuration properties and infrastructure wiring.

## Ownership

- `*Properties` - `@ConfigurationProperties` records, one per configuration group. Every
  property is validated at startup with `@Validated` and a bound, so a bad value fails the
  boot instead of the first request.
- `SecretStore` - the contract for a credential that is referenced by key and never
  stored in the database.
- `FileSecretStore` - the implementation: one file per secret, mode 0600 inside a 0700
  directory, named by the SHA-256 of the reference.
- `JpaConfig` - the datasource, entity manager factory and transaction manager.
- `AsyncConfig` - the task executors, each with a name, a bounded queue and explicit
  rejection handling.
- `SchedulingConfig` - the scheduler pool and its error handler.
- `BootstrapAdminInitializer` - sets the first administrator's password on a fresh
  install.
- `OpenApiConfig` - the OpenAPI definition, when the API docs are enabled.

## Local Contracts

- No `application.yml` value is a secret. Secrets arrive from the environment or the secret
  store and are bound through a properties record, so they never appear in a properties file.
- Every executor is named, bounded and has a documented rejection policy. The original
  declared a `taskExecutor` method without `@Bean`, so the pool it described never existed
  and the work silently ran on the common pool.
- Every executor method has `@Bean`. A method without it silently never exists.
- No bean is declared in two places. The original had two `BCryptPasswordEncoder` beans.
- Package scanning uses real package names, not the wildcard `com.pg.*` the original used.
- The scheduler pool has more than one thread and an error handler, so one failing job
  never stops the others and never stops the schedule.
- The secret store never caches a value and never logs one. `resolve` reads the file on
  every call and returns a fresh string.
- The bootstrap initializer never logs the password and never fails the boot because a
  user row is missing. It is a deployment convenience, not an activation mechanism: the
  application carries no licence, no token and no machine check.
- Hibernate never creates the schema. Flyway owns it; Hibernate only validates it, and
  only against MySQL. On H2 the dialect and `ddl-auto` are forced to the test values,
  because the MySQL dialect does not run there.

## Work Guidance

- Adding a property: add it to the properties record with a default and a validation rule,
  add it to `application.yml` with the same default, and reference it through the record
  rather than through `@Value`.
- A configuration change that alters behaviour needs a line in `docs/ARCHITECTURE.md`.

## Verification

- `./mvnw test` - the context load test fails if any bean is ambiguous, missing or invalid.
- The test profile runs on H2 in MySQL compatibility mode with Flyway enabled against the
  same migrations the production database gets.

## Child DOX Index

This package has no child DOX files.
