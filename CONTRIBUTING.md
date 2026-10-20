# Contributing

Thank you for wanting to help. This project is small and the rules are simple.

## The one rule that matters

**The gate ledger in `GATES.md` is the contract.** Every change has to leave every
gate passing. A pull request that does not is not ready, and that is not a judgement
about the author.

```bash
node ~/.agents/skills/unlazy/scripts/gate-check.mjs --status GATES.md
```

## Before you start

1. Read `AGENTS.md` at the repository root. It is the project-wide contract.
2. Read the `AGENTS.md` in the folder you are about to change. Each one is a local
   contract with its own rules.
3. Read `docs/ARCHITECTURE.md` if you are changing how something fits together.

The folder-level contracts are not decoration. They say which layer may call which,
what a tenant filter looks like, what a response body may carry, and which checks
apply. If a change needs a rule that is not written down, write the rule down in the
same pull request.

## Getting set up

You need Java 21 and Docker.

```bash
cp .env.development.example .env.development   # fill in the local database password
docker compose up -d db
./mvnw verify
```

The build runs on your machine. The `tools/sync-and-build.sh` script is only for the
project's own build host.

## Making a change

- Keep the layering. `web` calls `service`, `service` calls `repository` and the
  provider packages, `repository` calls `domain`. Nothing points the other way.
- Every service method takes the tenant id explicitly. A new endpoint that skips this
  is a security defect, not a style choice.
- Never put a credential in the database, in a properties file, or in a committed
  file. See `docs/SECURITY.md`.
- No `@GetMapping` changes state.
- Use `java.time`. No `SimpleDateFormat`, no `java.sql.Date`.
- Batch every bulk write. Bound every thread pool. Give every outbound call a
  timeout.
- Update the documentation in the same change. A document that describes removed
  behaviour is deleted, not annotated.

## Adding a database change

Write a new Flyway migration. Never edit an applied one.

```bash
src/main/resources/db/migration/V2__something_changed.sql
```

One statement per change where the target database is strict about it. Every table
needs a foreign key and an index on it. Every text column needs an explicit length
and a character set.

## Tests

Every change comes with a test. The suite must be hermetic: no network, no live
mailbox, no browser, no remote database. A local JDK `HttpServer` on loopback is the
only socket a test may open.

```bash
./mvnw test                       # the whole suite
./mvnw test -Dtest=SomeIT         # one class
```

Test names state the behaviour: `tenantACannotReadTenantBsClients`, not `testRead`.

## Pull requests

- One change per pull request. A pull request that fixes two unrelated things is two
  pull requests.
- Say what was wrong and what is different now. The reviewer reads the diff.
- If you fixed a defect the original also had, add it to
  `docs/MIGRATION-FROM-0.0.1.md`, so the reason survives.
- If you changed how something fits together, update `docs/ARCHITECTURE.md`.

## Reporting a security problem

Not a public issue. See `SECURITY.md`.

## Code of conduct

See `CODE_OF_CONDUCT.md`.
