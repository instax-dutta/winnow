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
- Every executor is named, bounded and has a documented rejection policy. A pool method
  without `@Bean` never exists, so the work would silently run on the common pool.
- Every executor method has `@Bean`. A method without it silently never exists.
- No bean is declared in two places. Two `BCryptPasswordEncoder` beans make the injected
  encoder ambiguous.
- Package scanning uses real package names, not a wildcard such as `com.example.*`, which picks up
  everything under the tree and breaks as soon as a package is added.
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

- A `@ConfigurationProperties` record has exactly **one** constructor. Spring binds through
  it, and a second convenience constructor makes the binding ambiguous, so the context fails
  to start with "No default constructor found". A test that needs a shorter form uses the
  full one.
- The credential store is chosen by `seo.secret-store.type`. The file store is the default.
  A new implementation is a `SecretStore` bean plus a `@ConditionalOnProperty` and a
  property in `SecretStoreProperties`. Nothing in the service layer changes.

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
