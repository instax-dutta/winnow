# Maintenance

The operating manual. How to build, deploy, migrate and troubleshoot `seo-0.1.0`.

## Prerequisites

| Requirement | Why | Notes |
|---|---|---|
| Java 21 | The build and the runtime | `maven.compiler.release` is 21. |
| Docker with the Compose plugin | The containerised run | Compose v2 (`docker compose`, not `docker-compose`). |
| MySQL 8 | The database | The schema uses `utf8mb4_0900_ai_ci`, `DATETIME(6)` and enforced foreign keys, all of which need MySQL 8. |
| Node 20 or newer | The verification scripts | `tools/verify/*.mjs` and `tools/harvest-secrets.mjs` are plain Node with no dependencies. |
| Maven | Not needed | The repository ships the wrapper. Never use a system Maven. |

## The two ways to run

### With Docker Compose

```bash
docker compose up -d --build
docker compose logs -f app
```

Two services. `db` is MySQL 8 on `127.0.0.1:3307`. `app` is the application on
`127.0.0.1:8083`, and it waits for `db` to be healthy. Flyway applies the schema on the
first boot.

Stop and remove it:

```bash
docker compose down          # keeps the volumes
docker compose down -v       # removes the volumes, and with them the database and the secrets
```

### With Maven against an external database

Point the application at a MySQL 8 you already have and run it from the source tree.

```bash
export DB_URL='jdbc:mysql://127.0.0.1:3306/emailsender_v01?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8'
export DB_USERNAME=seo_v01
export DB_PASSWORD='<the-database-password>'
export SECRET_STORE_PATH=./.secrets
export UPLOAD_DIR=./data/exports
./mvnw spring-boot:run
```

The database has to exist before the first start, and it has to be empty. Flyway creates the
schema inside it.

```sql
CREATE DATABASE emailsender_v01 CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'seo_v01'@'%' IDENTIFIED BY 'the-password';
GRANT ALL PRIVILEGES ON emailsender_v01.* TO 'seo_v01'@'%';
```

Do not run the application as `root` against the database. The original did, with the root
password in its configuration file. Use a dedicated user that owns only its own schema.

## The build commands

| Command | What it does |
|---|---|
| `./mvnw clean compile` | Compiles from a clean tree. No tests. |
| `./mvnw test` | Runs the whole test suite: the unit tests and the `*IT` integration tests. Surefire is configured to include both. |
| `./mvnw verify` | The gate. `test` plus the packaging, with the integration tests included. |
| `./mvnw package` | Builds `target/seo-0.1.0.jar`. Tests run unless `-DskipTests` is passed. |
| `./mvnw spring-boot:run` | Runs the application against the configured database. |
| `./mvnw test -Dtest=ClassName` | Runs one test class. |
| `./mvnw test -Dtest='TenantIsolationIT'` | The tenant isolation gate (G7). |
| `./mvnw test -Dtest='CsrfProtectionIT'` | The CSRF gate (G9). |

The test suite is hermetic. It opens no network connection, reaches no live mailbox and
starts no browser. The one socket it may open is a loopback `com.sun.net.httpserver`. The
test profile runs on H2 in MySQL-compatibility mode with Flyway on, so the same migrations
that run in production run in the suite.

## The remote build host

Builds run on `tejes@pelican`: Linux, 12 vCPU, Java 21, no GPU.

### The layout

| Path | What it holds |
|---|---|
| `~/seo-rebuild/app` | The working tree. `tools/sync-and-build.sh` unpacks into it. |
| `~/seo-rebuild` | The root of everything this project owns on that host. |

### The containers on that host

| Container | What it is | Where it listens |
|---|---|---|
| `seo-v01-mysql` | The dedicated MySQL 8 for this project. | `127.0.0.1:3307`, loopback only. |
| `seo-v01-app` | The application container for this project. | `127.0.0.1:8083`. |

The port matches the default `DB_URL` in `application.yml`, so a build on the host works
with no environment set at all.

### The rule

**Nothing else running on `pelican` may be started, stopped or reconfigured.** The host
does other work. This project's footprint is `~/seo-rebuild`, the `seo-v01-mysql` container
and the `seo-v01-app` container, and nothing outside that. No command in this repository may
reach outside it, no matter how convenient that would be. If a build seems to need a change
to another container or another service on that host, the answer is that it does not.

### The sync script

```bash
./tools/sync-and-build.sh            # sync + mvn compile
./tools/sync-and-build.sh test       # sync + mvn test
./tools/sync-and-build.sh verify     # sync + mvn verify
./tools/sync-and-build.sh package    # sync + mvn package
```

It excludes `.git`, `target`, `node_modules`, `.unlazy` and `reference` from the tar stream,
unpacks into `~/seo-rebuild/app` and runs Maven there with
`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`. `SEO_BUILD_HOST` and `SEO_BUILD_DIR`
override the host and the directory.

## The database lifecycle

### Applying a migration

Flyway owns the schema. Migrations live in `src/main/resources/db/migration/` and are named
`V<n>__<snake_case_description>.sql`.

1. Write the new file. Never edit an applied one.
2. Run the application once, or run `./mvnw verify`, which applies the migrations to a clean
   H2 schema in MySQL mode.
3. Apply it to the real database by starting the application. Flyway runs at boot.

The rules a migration has to satisfy:

- Every table is `utf8mb4_0900_ai_ci`.
- Every text column gets an explicit length. Never a length that truncates a realistic
  value.
- Every foreign key gets an index.
- Every tenant-owned table gets an index on `owner_user_id`.
- Every tenant-owned table gets `created_at`, `updated_at`, `created_by`, `updated_by` and
  `deleted_at`.
- A destructive change is split: add the column, backfill it, then drop the old one.

### V1 is frozen

`V1__baseline.sql` is the pre-release baseline. It creates the whole 0.1.0 schema and it is
**frozen at 0.1.0**. It is not to be edited, because it is the record of what the 0.1.0
release is. A change to the schema at 0.1.0 is a new `V2__*.sql`.

### If V1 has to change anyway

Flyway stores a checksum of every applied migration. Change `V1__baseline.sql` and the
checksum no longer matches, and the next start fails with a validation error rather than
applying anything.

The development database then has to be recreated, because Flyway cannot repair its own
history against a changed file:

```bash
docker compose down -v
docker compose up -d --build
```

`down -v` removes the `seo-v01-db` volume, so the database starts empty and Flyway applies
`V1` afresh. Any data in it is gone. This is only acceptable for a development database.

The alternative, when the change is needed on a database that holds data, is to re-align the
stored checksum with `flyway repair` (the Flyway CLI, or the `flyway-maven-plugin` pointed at
the same URL and credentials), followed by a careful review of what the changed migration now
does to the existing rows.

## The scheduled jobs

Three jobs run on the scheduler pool. The pool has four threads and an error handler, so one
failing job never stops the others and never stops the schedule. The original used a
single-threaded scheduler, so a stalled IMAP connection delayed every other job.

| Job | Schedule key | Default | What it does |
|---|---|---|---|
| `scheduler/OutboxDispatchJob.dispatchDue` | `seo.scheduler.outbox-delay-ms` | 30 s | Claims the outbox rows whose `scheduled_at` has passed, with a conditional status update so a row is never sent twice, and sends each one through the provider adapter of its sending account. |
| `scheduler/InboxPollJob.poll` | `seo.scheduler.inbox-delay-ms` | 300 s (5 min) | Polls the mailbox of every enabled SMTP account over IMAP and stores the new messages. |
| `scheduler/ExtractionJob.advance` | `seo.scheduler.extraction-delay-ms` | 60 s | Closes every extraction run that was left behind: a queued run is closed as failed, a running run that passed the run timeout is resumed. |

Two more properties control the behaviour rather than the timing:

| Property | Default | What it controls |
|---|---|---|
| `seo.scheduler.outbox-batch-size` | `50` | The number of outbox rows claimed per dispatch. |
| `seo.scheduler.extraction-run-timeout-minutes` | `30` | The age at which a `RUNNING` run is taken over and resumed. |

### Changing a schedule

The schedule is a property, not a constant. Change the value in `application.yml` and, if it
is environment-specific, add the environment variable:

```bash
SCHEDULER_OUTBOX_DELAY_MS=60000 docker compose up -d
```

| Variable | Property |
|---|---|
| `SCHEDULER_OUTBOX_DELAY_MS` | `seo.scheduler.outbox-delay-ms` |
| `SCHEDULER_INBOX_DELAY_MS` | `seo.scheduler.inbox-delay-ms` |
| `SCHEDULER_EXTRACTION_DELAY_MS` | `seo.scheduler.extraction-delay-ms` |
| `SCHEDULER_EXTRACTION_TIMEOUT_MINUTES` | `seo.scheduler.extraction-run-timeout-minutes` |
| `SCHEDULER_OUTBOX_BATCH_SIZE` | `seo.scheduler.outbox-batch-size` |

A schedule change does not need a migration. It does need a restart of the application.

**Do not put the scheduler in a controller.** The original had
`@Scheduled(fixedRate = 60000)` on a controller method, which processed every tenant's
queued mail in one batch and lost the remaining groups when one sender threw.

## The secret store

### Where it lives

`SECRET_STORE_PATH`. The default is `./.secrets` in the source tree and `/app/.secrets` in
the container, where it is on the `seo-v01-secrets` volume.

One file per secret. The file name is the SHA-256 hex of the reference, so the directory
listing discloses nothing about what is stored and a reference can never escape the
directory. The file is mode 0600 inside a 0700 directory. A write goes to a temporary file
that is then renamed, so a reader never sees a half written secret.

The directory is **not** in `.gitignore` as a rule, so check before committing anything in
the working tree. Nothing in it belongs in a commit.

### How a value gets in

The value is entered through the application. When an SMTP or Gmail account is created or
updated, the credential in the request is stored in the secret store and the account row
keeps the reference. The credential is write-only: it is never returned, never logged and
never held on a field.

To put a value in by hand, write it to the file the reference hashes to:

```bash
# The reference for an account is "<namespace>/<key>".
printf '%s' 'the-smtp-password' > .secrets/$(printf '%s' 'smtp/3' | shasum -a 256 | cut -d' ' -f1)
chmod 600 .secrets/*
```

Take the reference from the `secret_ref` column of the account row.

### How a value is rotated

1. Write the new value over the file the reference hashes to. The store reads from disk on
   every `resolve` and never caches, so the new value takes effect on the next send without
   a restart.
2. Or, for a value that is entered through the application, update the account. The
   service writes the new value to the store and keeps the same reference.
3. Rotate the credential at the provider as well. Rotating the value in the store does not
   rotate it at Google or at the mail provider.

### Deleting the store loses the credential

Deleting the store file deletes the credential. The account row keeps a reference to a
value that no longer exists, and the send fails with "the account has no credential". There
is no recovery from the store, because the value is not anywhere else. If a credential has to
be recovered, it has to be re-entered, and the provider credential has to be reissued as
well.

In Compose, the store is on the `seo-v01-secrets` volume, so `docker compose down` keeps it
and `docker compose down -v` deletes it.

## Troubleshooting

### The application will not start

1. Read the log: `docker compose logs app`, or the console output of
   `./mvnw spring-boot:run`.
2. **"Flyway validation failed"** means a migration file changed after it was applied. See
   the database lifecycle above.
3. **"Schema-validation: missing table"** means Flyway did not run, usually because the
   application could not reach the database. Check the next item.
4. **"Bean creation failed"** or **"no qualifying bean"** usually means a configuration
   property is missing or out of range. Every property record is validated at startup with a
   bound, so a bad value fails the boot rather than the first request. The message names the
   property.
5. **A port already in use** means an earlier instance is still running. `docker compose
   down`, or find the process on 8083.

### The database will not connect

1. Is the database up? `docker compose ps`, and
   `docker compose exec db mysqladmin ping -h 127.0.0.1 -p"$MYSQL_ROOT_PASSWORD"`.
2. Is the URL right? In Compose the application reaches the database at the service name
   `db`, not at `127.0.0.1`: `jdbc:mysql://db:3306/emailsender_v01?...`. From the host, it
   is `127.0.0.1:3307`.
3. **"Public Key Retrieval is not allowed"** means the user authenticates with
   `caching_sha2_password` and the URL is missing `allowPublicKeyRetrieval=true`. The
   default URL in `application.yml` has it.
4. **"Access denied for user"** means the credentials do not match. `DB_USERNAME` and
   `DB_PASSWORD` must be the same values the database was created with.
5. **The collation is wrong** and MySQL complains about the schema. The database has to be
   created with `CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci`. The Compose service
   passes `--character-set-server=utf8mb4 --collation-server=utf8mb4_0900_ai_ci`.
6. The build host's database is the `seo-v01-mysql` container on `127.0.0.1:3307`. Do not
   point a build at any other database on that host.

### The migrations do not apply

1. **"Found N migrations, but the schema history table is empty"** and the database already
   has tables: the database was created outside Flyway. Either drop the schema and let
   Flyway build it, or set `spring.flyway.baseline-on-migrate` deliberately for that
   database.
2. **A migration fails on a clean schema.** The integration tests apply the migrations to a
   clean H2 schema in MySQL mode, so a migration that is invalid fails the build. Run
   `./mvnw verify` and read the first failure.
3. **"Checksum mismatch"** means an applied migration file was edited. See the database
   lifecycle.
4. **The application is using the wrong database.** Check `DB_URL` and
   `spring.datasource.url`. The Flyway history table lives in the schema the URL names.

### The login keeps failing

1. **Every message is the same.** "Sign in failed. Check your username and password and try
   again." covers a wrong password, an unknown user, a disabled account and a failed
   lockout. There is no diagnostic difference by design.
2. **The account is locked.** After `seo.security.max-failed-logins` (default 5) consecutive
   failures the account is locked for `seo.security.lock-duration` (default 15 minutes).
   Wait it out, or clear it:
   ```sql
   UPDATE app_user SET failed_login_count = 0, locked_until = NULL WHERE username = 'admin';
   ```
3. **The password was never set.** With `BOOTSTRAP_PASSWORD` empty, the seeded account keeps
   the password `change-me-now` from `V1__baseline.sql`. Sign in with that, or set
   `BOOTSTRAP_PASSWORD` and restart the application.
4. **The bootstrap user does not exist.** The initializer logs a warning and the boot
   carries on. It does not create the account. Create the row, or point
   `BOOTSTRAP_USERNAME` at an account that exists.
5. **The bootstrap user has no role.** The initializer grants `ROLE_ADMIN` when the role is
   missing. Check `app_user_role` if the account can sign in but can do nothing.
6. **The account is disabled.** `app_user.status` must be `ACTIVE`.

### The extraction finds nothing

1. **The sheet has the domains in the wrong column.** The domains are read from column A.
2. **The sheet is not a spreadsheet.** Only `.xlsx` and `.xls` are accepted, and the guard
   checks the extension and the declared content type before the service is called.
3. **The host is not reachable.** The crawler fetches `https://<domain>`. A domain that
   does not resolve, does not answer, or answers over plain HTTP only will produce nothing.
4. **The host resolves to a private address.** `UrlAllowList` rejects it, and the rejection
   is logged. This is the control working, not a bug.
5. **The allow-list is restricting more than expected.** With
   `seo.scraper.allowed-host-suffixes` set and
   `seo.scraper.allow-arbitrary-hosts` false, only a listed host may be crawled. Check both.
6. **The page has no addresses in its HTML.** The crawler reads the body. It does not
   execute the page, so an address that is only in the rendered DOM is not found.
7. **The run is stuck.** `ExtractionJob` closes a run that was left behind after
   `seo.scheduler.extraction-run-timeout-minutes`. Check the run's status and its
   `error_detail`.
8. **The response was too large.** A body over `seo.extraction.max-response-bytes` (2 MiB)
   is cut off.

## The verification

### GATES.md

`GATES.md` is the authoritative ledger. It is an unlazy ledger: every runnable gate has a
`CHECK:` command and an `EXPECT:` token, and a gate counts as met only when the command
exits zero and the expectation matches. The gates were written before the implementation
and are not edited to match the outcome.

| Gate | What it proves | Command | Expectation |
|---|---|---|---|
| G1 | The project compiles from a clean checkout. | `./mvnw -q -B -DskipTests clean compile` | `BUILD SUCCESS` |
| G2 | The whole test suite passes. | `./mvnw -B test` | `Tests run:` |
| G3 | The packaged application starts and serves the login page. | `node tools/verify/app-boot.mjs` | `APP-BOOT-PASS` |
| G4 | No credential-shaped literal exists in the source tree. | `node tools/verify/no-secrets.mjs` | `SECRET-SCAN-PASS` |
| G5 | The code obeys the hard rules in the root `AGENTS.md`. | `node tools/verify/code-hygiene.mjs` | `HYGIENE-PASS` |
| G6 | The DOX tree is complete, consistent and reachable from the root. | `node tools/verify/dox-tree.mjs` | `DOX-TREE-PASS` |
| G7 | One authenticated tenant cannot read or write another tenant's data. | `./mvnw -B test -Dtest='TenantIsolationIT'` | `Tests run:` |
| G8 | Every Flyway migration applies to a clean MySQL 8 schema. | `node tools/verify/migrations.mjs` | `MIGRATIONS-PASS` |
| G9 | Every HTTP endpoint that mutates state rejects a request without a CSRF token. | `./mvnw -B test -Dtest='CsrfProtectionIT'` | `Tests run:` |
| G10 | The original artifact's secrets are still inventoried for the owner. | `node tools/harvest-secrets.mjs` | `findings:` |
| G11 | The architecture, migration, security and maintenance documents exist and are non-empty. | `node tools/verify/docs.mjs` | `DOCS-PASS` |
| G12 | The original's behaviour parity surface is documented, not guessed. | `node tools/verify/parity.mjs` | `PARITY-PASS` |
| G13 | A fresh clone of the repository builds without the local Maven cache. | `node tools/verify/clean-build.mjs` | `CLEAN-BUILD-PASS` |

G1, G2, G7 and G9 need a JDK 21 on the path and network access for the first dependency
resolution. G3 and G8 need the dedicated MySQL 8 container on the build host, and both are
skipped with an explicit notice rather than a silent pass when the database is unreachable.
G10 writes `SECRETS.txt`, which is git-ignored; the gate asserts the count, not the content.

### The verification scripts

| Script | What it checks |
|---|---|
| `tools/verify/docs.mjs` | The four documents of this set exist and are not empty. |
| `tools/verify/parity.mjs` | The migration document names every original table and every original endpoint. |
| `tools/verify/dox-tree.mjs` | The DOX tree is complete and consistent: every folder that is a boundary has an `AGENTS.md`, every child index matches what is on disk, and the root reaches everything. |

And the rest of the set, which the other gates use: `app-boot.mjs`, `no-secrets.mjs`,
`code-hygiene.mjs`, `migrations.mjs` and `clean-build.mjs` in `tools/verify/`, plus
`tools/harvest-secrets.mjs`.

All of them are plain Node with no dependencies. Run one directly:

```bash
node tools/verify/docs.mjs
node tools/verify/parity.mjs
node tools/verify/dox-tree.mjs
```

### The gate lint

The ledger itself is checked by the unlazy tooling:

```bash
node ~/.agents/skills/unlazy/scripts/gate-lint.mjs GATES.md
node ~/.agents/skills/unlazy/scripts/gate-check.mjs --status GATES.md
node ~/.agents/skills/unlazy/scripts/gate-check.mjs --approve GATES.md   # only for commands you wrote
```

## The DOX chain

Every meaningful change needs a DOX pass before it is done. Read the root `AGENTS.md`, then
walk from the root to the path you are changing and read every `AGENTS.md` on the way. The
nearest one is the local contract.

| Path | Owns |
|---|---|
| `AGENTS.md` | The project rules, the open source decision, the stack contract, the hard rules. |
| `docs/AGENTS.md` | The documentation contract: what each document covers and how it is written. |
| `docs/reverse-engineering/` | The decompilation record. Never edited to match the rebuild. |
| `src/main/java/com/pg/seo/AGENTS.md` | The layering rule, the tenant model, the entity rules, the error handling. |
| `src/main/java/com/pg/seo/<package>/AGENTS.md` | The contract of each package. |
| `src/main/resources/AGENTS.md` | The template, static asset, migration and configuration rules. |
| `src/test/AGENTS.md` | The test suite conventions. |
| `tools/AGENTS.md` | The repository tooling rules. |

When the code changes, the matching document changes in the same commit. A document that
describes behaviour that no longer exists is deleted, not annotated.
