# Architecture

How `seo-0.1.0` is built. This document records the decisions, not the history. Where a
decision exists because the original did the opposite, the original's behaviour is cited
against `docs/reverse-engineering/`.

## The layering rule

```
web  ->  service  ->  repository  ->  domain
         |
         +-> mail | extraction   (reached through service, never directly)
```

| Layer | May call | May not |
|---|---|---|
| `web` | `service`, and the security package for the principal | `repository`, `mail`, `extraction` |
| `service` | `repository`, `mail`, `extraction`, other services | `web` |
| `repository` | `repository`, `domain` | anything else |
| `domain` | nothing | everything |

`scheduler` sits beside `service`: it calls services, it does not contain business rules.
`config` sits beside everything and contains no domain logic. `security` is a cross-cutting
package that `web` reads for the principal.

**Why.** The original had no boundary that was enforced anywhere. Controllers reached
repositories and entities directly, so the entity was the wire format, and the entity carried
the plaintext SMTP password, the OAuth client secret and the refresh token straight into the
JSON response of `POST /getEmailMaster` and `POST /getAllGmail`
(`reference/decompiled/com/pg/seo/service/EmailService.java:132`,
`reference/decompiled/com/pg/seo/service/GmailService.java:166`). A boundary that is only a
convention does not hold. The package rule is written into each package's `AGENTS.md`, is
enforced by the fact that the services return DTOs rather than entities, and is checked by
the tests in `src/test/java/com/pg/seo/integration/`.

## The package map

| Package | One line responsibility |
|---|---|
| `domain/` | JPA entities, enums and the audited base types. Knows nothing about how data is fetched or shown. |
| `repository/` | Spring Data JPA repositories, one per aggregate. The only place that writes SQL and the backstop of the tenant filter. |
| `service/` | Business rules and transaction boundaries. A use case lives end to end here, and the tenant filter is applied here. |
| `web/` | The HTTP contract. Controllers, the response envelope, the error mapping, the upload guard. |
| `model/` | The request and response shapes. A DTO never carries a credential. |
| `security/` | Authentication, authorization, the principal model, the session and CSRF policy. |
| `mail/` | Provider adapters (SMTP and Gmail), MIME construction, template rendering, quota accounting. |
| `extraction/` | Domain crawling, the URL allow-list, the address extraction, the Amazon scraper client. |
| `scheduler/` | Scheduled job triggers and their idempotency. Decides when work happens, never what it does. |
| `config/` | Bean definitions, typed property records, the secret store, the datasource, the executors, the scheduler pool. |

The rules for each package are in `src/main/java/com/pg/seo/<package>/AGENTS.md`.

## The tenant model

**The authenticated user is the tenant.** There is no organisation, no account, no
`tenant_id` column, and no way for a request to choose a tenant.

- `security/AuthPrincipal` carries `userId`, `username` and the granted authorities.
- `web/AuthPrincipalResolver` reads the principal out of the security context. A request
  parameter can never set it.
- Every service method takes the owner id as its first argument, explicitly. It never reads
  it from a thread-local, a session attribute or a request-scoped bean.
- Every repository method that reads or writes tenant-owned data takes the owner id as well.
  The method name states it: `findByOwnerUserIdAndDeletedAtIsNull`.
- The entity holds `owner_user_id`, and every tenant-owned table carries an index on it.

The repository filter is the **backstop**, not the mechanism. The mechanism is the explicit
owner id that the service passes down. The backstop matters because it is the layer that
cannot be forgotten: a service that forgot to filter would still call a repository method
that requires the owner id.

The original did the opposite. It kept the tenant in a session attribute named `loginId`,
read it out of `HttpSession` inside every controller, and passed it to a service that
ignored it in eleven places. `reference/decompiled/com/pg/seo/service/EmailService.java:463`
(`removeClient`) looked a group up with `findByGroupCode(groupCode)` and no tenant, so one
tenant could soft-delete another tenant's clients. The rebuild has no session tenant at
all, and `src/test/java/com/pg/seo/integration/TenantIsolationIT.java` asserts the isolation
across every repository method and every mutating service.

**Global entities.** There are none in 0.1.0. `app_role` is the closest thing: it holds the
role catalogue and carries no `owner_user_id`. It is a global entity because a role code
means the same thing for every tenant, and it is read-only through the application.

## The data model

The schema is owned by Flyway. `src/main/resources/db/migration/V1__baseline.sql` is the
baseline and the only migration at 0.1.0. Hibernate never creates a table:
`spring.jpa.hibernate.ddl-auto` is `validate`.

| Table | Purpose |
|---|---|
| `app_user` | A person who can sign in. Carries the BCrypt hash, the status, the failed-login counter and the lock window. |
| `app_role` | The role catalogue: `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_OPERATOR`, `ROLE_VIEWER`. |
| `app_user_role` | Which user holds which role. |
| `client_group` | A named group of contacts, owned by a tenant. |
| `client` | A contact: name, email address, optional web link, optional group. |
| `email_template` | The subject and the HTML body of a message, plus the approval state, the version and the identity of the approver. |
| `smtp_account` | An SMTP sending account: host, port, protocol, auth method, username, and a `secret_ref`. |
| `gmail_account` | A Google OAuth sending account: client id, scopes, and a `secret_ref` plus a `refresh_token_ref`. |
| `campaign` | One send job: the sending account, the target type, the template, the counts and the state. |
| `delivery` | One recipient of one campaign, with its own status, provider message id and error. |
| `outbox` | One scheduled message, waiting for its `scheduled_at`. |
| `extraction_run` | One extraction job: the source file, the domain count, the state and the error. |
| `extraction_result` | One address found by one run, with a hash of the normalised address for the dedupe constraint. |
| `inbox_message` | One captured inbound message. |

### The decisions that matter

**Soft delete by `deleted_at`.** Every tenant-owned table has a `deleted_at DATETIME(6) NULL`
column and an explicit `deletedAt IS NULL` predicate in the repository method. The original
used an `isActive int` plus a Hibernate `@Where(clause = "isActive =1")` class filter, which
is silently bypassed by every native query. The original had four native queries
(`reference/decompiled/com/pg/seo/repository/`), all `select *`, and the filter did not apply
to any of them, so "deleted" rows were visible and deletable depending on the path taken.

**Real foreign keys.** Every relation is a declared `FOREIGN KEY` with an explicit
`ON DELETE` policy. The original had no foreign key on any table. `email_extractor_data` held
a bare `emailExtractorId` with no constraint and no index, so it accumulated orphan rows;
`client_master.groupCode` referenced a non-unique, non-primary column of `client_group`.
The rebuild points `client.group_id` at `client_group(id)` and adds the index the original
lacked.

**Unique constraints where the original had none.** The original had zero unique constraints
across its thirteen tables. The rebuild has them where the data is meant to be single:
`uk_app_user_username`, `uk_app_role_code`, `uk_client_group_code`, `uk_smtp_account_email`,
`uk_gmail_account_email`, `uk_extraction_result_run_email` on `(run_id, email_hash)`, and
`uk_inbox_account_message`.

**`DATETIME(6)` for every timestamp.** The original stored dates as `VARCHAR(255)` in
twelve columns and used `java.sql.Date` in five more, which discards the time of day. A
campaign that started at 23:59 and finished at 00:01 was indistinguishable from one that
started and finished at midnight. Every timestamp in the rebuild is `Instant` mapped to
`DATETIME(6)`.

**`utf8mb4_0900_ai_ci` everywhere.** Every table declares
`ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci`. The original
inherited whatever the server default was, with no charset in the JDBC URL and no explicit
per-table collation in a hand-created schema.

**Enums as `VARCHAR`, not MySQL `ENUM`.** A status is a Java enum mapped to `VARCHAR(32)`,
so a new status is a code change rather than a migration. The original had five free-text
status columns with no check constraint.

**Text lengths that fit.** The original had `VARCHAR(255)` for a full CKEditor HTML
template and for a full email body, so both truncated. The rebuild uses `MEDIUMTEXT` for
`email_template.content_html`, `outbox.body_html` and `inbox_message.body_html`, `TEXT` for
error detail, and `VARCHAR(254)` for an address.

**No credential column.** `smtp_account.secret_ref` and
`gmail_account.secret_ref` / `refresh_token_ref` are references into the secret store. There
is no column anywhere that holds a password, a client secret or a token.

**Audit columns on every tenant-owned table.** `created_at`, `created_by`, `updated_at`,
`updated_by` and `deleted_at`. The original had no audit columns at all on
`email_queue`, `email_inbox`, `role_security` and `login_role_security`, and no
`modified_by` / `modified_date` on four more.

## The send flow

Two paths exist. They are separate because they were separate in the original and they use
different providers.

### Path A, a campaign against a group or a contact list

Synchronous on the request thread until the campaign and its delivery rows are recorded.
The dispatch of those delivery rows is not implemented yet; the note at the end of this
section says what is in place and what is not.

1. **Campaign creation** (synchronous, `web/CampaignController` -> `service/CampaignService.create`).
   The service loads the template by `id` **and** `ownerUserId` and refuses it unless its
   status is `APPROVED`. The original sent whatever template came out of the lookup, with no
   status check, and the lookup was not tenant-scoped.
2. **Recipient resolution** (synchronous, `service/ClientService.resolveTargets`). For a
   group target the members of the tenant's group are read; for a contact target the named
   contacts are read, each within the tenant. The original resolved recipients with
   `findByClientMasterId` and no tenant
   (`reference/decompiled/com/pg/seo/service/EmailService.java:331`).
3. **Quota check** (synchronous, `mail/QuotaService.assertWithinQuota`). The count of what
   the tenant already sent through that account in the current hour, day and month is
   compared against the configured limits. The original checked one number, the sum over the
   current calendar month, against a hard-coded 50 000, and called it the daily limit
   (`reference/decompiled/com/pg/seo/service/EmailService.java:114`). The rebuild has an
   hourly, a daily and a monthly limit, per tenant and per account, and the daily limit is
   a daily limit.
4. **The campaign row** (synchronous, one transaction). One `campaign` row with the recipient
   count and the status `QUEUED`.
5. **The delivery rows** (synchronous, one transaction, batched). One `delivery` row per
   recipient, written in batches of 200. The original wrote one row per recipient with a
   separate `save()` and did three round trips per recipient inside the send loop
   (`reference/decompiled/com/pg/seo/service/EmailService.java:496`).
6. **The provider adapter** (`mail/SmtpMailSender` or `mail/GmailMailSender`). The adapter
   resolves the credential from the secret store **at call time**, holds it only for the
   duration of the call and drops it. Every SMTP connection has explicit connect, read and
   write timeouts. One `MimeMessage` is built per recipient. Every message carries a
   `Message-ID`, a plain-text alternative and a correct `From`. A failure message carries the
   type of the failure and never the verbatim response of the mail server.
7. **The delivery record** (after the send, outside the transaction). Success writes
   `SENT` and the provider message id; failure writes `FAILED` and the error. A per-recipient
   failure never fails the batch. The original recorded only `SUCCESS` or `FAILURE` with no
   error detail at all, so a failed send could not be diagnosed
   (`reference/decompiled/com/pg/seo/service/EmailService.java:129`).

**What path A does not do yet.** Steps 1 to 5 are implemented. Steps 6 and 7 are the
primitives the `mail` package provides and that `OutboxDispatchJob` uses, but no job
dispatches a campaign's delivery rows yet: a delivery created by
`CampaignService.create` stays in `QUEUED`. The primitives that a campaign dispatcher needs
are in place, and the job itself is the piece that is missing. Path B is the path that
sends today.

### Path B, the scheduled outbox

1. **Scheduling** (synchronous, `service/OutboxService.schedule`). One `outbox` row per
   message with `scheduled_at`, the target address, the draft or body and the sending
   account.
2. **Claim** (scheduled, `OutboxService.claimDue`). The claim is a conditional update: the
   status is part of the `WHERE` clause, so only one worker moves a row out of `SCHEDULED`.
   `attempt_count` is incremented on the claim.
3. **Dispatch** (scheduled, `OutboxDispatchJob.dispatchDue`). The sending account is
   resolved **inside the tenant of the row**, then the adapter sends. On success the row is
   marked sent, on failure it is marked failed with the error, and the job carries on.

### What is synchronous and what is scheduled

| Step | When |
|---|---|
| Campaign creation, recipient resolution, quota check | Synchronous, on the request thread |
| The `campaign` row and the `delivery` rows | Synchronous, one transaction, batched |
| Outbox scheduling | Synchronous, on the request thread |
| Claiming and dispatching a due outbox row | Scheduled, every 30 s by default |
| The mailbox poll | Scheduled, every 5 minutes by default |
| Advancing an extraction run | Scheduled, every 60 s by default |
| Dispatching a campaign's delivery rows | Not implemented yet. See path A above. |

## The extraction flow

1. **Upload** (synchronous, `web/ExtractionController` -> `web/UploadGuard`). The file is
   parsed **in memory**. The guard checks the type and the size at the edge, before the
   service is called. The multipart limit is 20 MB, down from the original's 128 MB
   (`reference/decompiled/com/pg/seo/controller/EmailController.java:379`).
2. **The domain list** (synchronous, `service/ExtractionService.start`). Column A of the
   sheet holds the domains. The run row is created first, with the state `QUEUED`, and the
   source file name is recorded. The run is deliberately not transactional, because the
   crawl is an outbound call and must not hold a transaction open.
3. **The allow-list** (before every fetch, `extraction/UrlAllowList`). Three rules in
   order: the scheme must be `https` or `http`; the host must be on
   `seo.scraper.allowed-host-suffixes` unless `allow-arbitrary-hosts` is on; and **every**
   address the host resolves to must be a public address. The private ranges are blocked
   unconditionally and cannot be switched off.
4. **The crawl** (scheduled, `scheduler/ExtractionJob.advance` -> `extraction/ExtractionWorker`).
   A fixed pool of `seo.extraction.worker-threads` (default 8) with a bounded queue of
   `seo.extraction.queue-capacity` (default 500). The domains are submitted in chunks, not
   one task per row. Each fetch has an explicit connect timeout, a read timeout and a
   response size cap of `seo.extraction.max-response-bytes` (default 2 MiB). A redirect is
   followed at most `seo.extraction.max-redirects` times (default 2) and each hop is
   re-validated against the allow-list. A failed domain is a failed domain and does not fail
   the run.
5. **The regex** (`extraction/HttpDomainCrawler`). The body is matched against a
   conservative address pattern, and an address is kept only when it belongs to the domain
   that was crawled. The crawler reads the body. It never executes the page, and no headless
   browser is used for domain crawling.
6. **The batch write** (`service/ExtractionService` -> `ExtractionSink`). The addresses are
   written in batches of `seo.extraction.write-batch-size` (default 200). The dedupe is a
   hash of the normalised address with a unique constraint on `(run_id, email_hash)`, and
   the dedupe set is scoped to the run. The original kept a process-wide map that was never
   cleared, so a second run over the same domain returned nothing
   (`reference/decompiled/com/pg/seo/service/NewEmailExtractorService.java:50`).
7. **The status** (`service/ExtractionService.closeRun`). A run always terminates in
   `SUCCESS`, `PARTIAL` or `FAILED`. It is never left `RUNNING`. The original left the run
   at `IN PROGRESS` whenever an exception escaped
   (`reference/decompiled/com/pg/seo/service/NewEmailExtractorService.java:190`).

## The security model in summary

The detail is in `docs/SECURITY.md`. The shape:

- **The credential never lands in the database.** The entity holds a `secret_ref`. The
  adapter resolves the value from the secret store at call time and drops it. No DTO carries
  a credential. No log line carries one.
- **The tenant is the authenticated user.** The owner id is passed down explicitly and
  filtered again in the repository.
- **CSRF is enabled.** A missing token is answered with a redirect to the login page; a
  wrong token, or a caller with no role, is answered with 403.
- **No state change happens over GET.** Every mutation is a `POST`, `PUT` or `DELETE`.
- **Every outbound URL is allow-listed.** A spreadsheet cell can never become an arbitrary
  request.
- **No licence, no activation, no machine fingerprinting.** Authentication and roles are
  not licensing and stay.

## The configuration surface

`application.yml` holds defaults only. Every secret comes from the environment or from the
secret store.

### `seo.secret-store`

| Property | Default | What it controls |
|---|---|---|
| `seo.secret-store.path` | `${SECRET_STORE_PATH:./.secrets}` | The directory the secret store keeps its files in. |

### `seo.storage`

| Property | Default | What it controls |
|---|---|---|
| `seo.storage.upload-dir` | `${UPLOAD_DIR:./data/exports}` | Where generated exports are written. |

### `seo.quota`

| Property | Default | What it controls |
|---|---|---|
| `seo.quota.daily-send-limit` | `500` | The number of messages a tenant may send through one account in a calendar day. |
| `seo.quota.hourly-send-limit` | `50` | The same for a calendar hour. |
| `seo.quota.monthly-send-limit` | `50000` | The same for a calendar month. |

### `seo.mail`

| Property | Default | What it controls |
|---|---|---|
| `seo.mail.connect-timeout` | `15s` | The SMTP connect timeout. |
| `seo.mail.read-timeout` | `30s` | The SMTP read timeout. |
| `seo.mail.write-timeout` | `30s` | The SMTP write timeout. |
| `seo.mail.per-account-daily-limit` | `500` | The daily limit applied per sending account. |
| `seo.mail.per-account-hourly-limit` | `50` | The hourly limit applied per sending account. |
| `seo.mail.batch-size` | `200` | The number of recipients written per batch. |
| `seo.mail.retry-backoff` | `5s` | The wait before a retry. |

The original left the SMTP timeouts at the infinite default
(`reference/decompiled/com/pg/seo/service/EmailService.java:82`), so one stalled server
blocked a thread forever.

### `seo.extraction`

| Property | Default | What it controls |
|---|---|---|
| `seo.extraction.connect-timeout` | `5s` | The connect timeout of a crawl. |
| `seo.extraction.read-timeout` | `10s` | The read timeout of a crawl. |
| `seo.extraction.max-response-bytes` | `2097152` | The cap on the size of a crawled response. |
| `seo.extraction.max-redirects` | `2` | The number of redirects that are followed, each re-validated. |
| `seo.extraction.worker-threads` | `8` | The size of the crawl pool. |
| `seo.extraction.queue-capacity` | `500` | The size of the crawl queue. |
| `seo.extraction.per-domain-timeout-seconds` | `15` | The budget for one domain. |
| `seo.extraction.max-domains-per-run` | `5000` | The largest number of domains one run accepts. |
| `seo.extraction.write-batch-size` | `200` | The number of addresses written per batch. |

### `seo.scraper`

| Property | Default | What it controls |
|---|---|---|
| `seo.scraper.base-url` | `${SCRAPER_URL:http://127.0.0.1:3000}` | The base URL of the Amazon scraper sidecar. |
| `seo.scraper.connect-timeout` | `5s` | The connect timeout to the sidecar. |
| `seo.scraper.read-timeout` | `15s` | The read timeout to the sidecar. |
| `seo.scraper.allow-arbitrary-hosts` | `${SCRAPER_ALLOW_ARBITRARY_HOSTS:true}` | When false, only a host matching `allowed-host-suffixes` may be crawled. |
| `seo.scraper.allowed-host-suffixes` | (empty) | The operator-controlled host restriction, on top of the unconditional private-range block. |

### `seo.bootstrap`

| Property | Default | What it controls |
|---|---|---|
| `seo.bootstrap.username` | `${BOOTSTRAP_USERNAME:admin}` | The account the bootstrap password is applied to. |
| `seo.bootstrap.password` | `${BOOTSTRAP_PASSWORD:}` | The password applied on start. Empty leaves the account untouched. |

### `seo.scheduler`

| Property | Default | What it controls |
|---|---|---|
| `seo.scheduler.outbox-delay-ms` | `${SCHEDULER_OUTBOX_DELAY_MS:30000}` | The delay between outbox dispatches. |
| `seo.scheduler.inbox-delay-ms` | `${SCHEDULER_INBOX_DELAY_MS:300000}` | The delay between mailbox polls. |
| `seo.scheduler.extraction-delay-ms` | `${SCHEDULER_EXTRACTION_DELAY_MS:60000}` | The delay between extraction advances. |
| `seo.scheduler.extraction-run-timeout-minutes` | `${SCHEDULER_EXTRACTION_TIMEOUT_MINUTES:30}` | The age at which a `RUNNING` run is taken over and resumed. |
| `seo.scheduler.outbox-batch-size` | `${SCHEDULER_OUTBOX_BATCH_SIZE:50}` | The number of outbox rows claimed per dispatch. |

### `seo.security`

| Property | Default | What it controls |
|---|---|---|
| `seo.security.max-failed-logins` | `${MAX_FAILED_LOGINS:5}` | The number of consecutive failures that lock the account. |
| `seo.security.lock-duration` | `${LOCK_DURATION:15m}` | How long the account stays locked. |

### `seo.jpa`

| Property | Default | What it controls |
|---|---|---|
| `seo.jpa.show-sql` | `false` | Whether SQL is logged. The original had `hibernate.show_sql = "true"`, which printed every recipient address and every scraped lead to stdout (`reference/decompiled/com/pg/seo/cfg/JpaConfigurationMySql.java:80`). |

### The Spring properties that matter

| Property | Value | What it controls |
|---|---|---|
| `server.port` | `${SERVER_PORT:8083}` | The port. |
| `server.error.include-message` | `never` | No internal message is returned to a caller. |
| `server.error.include-stacktrace` | `never` | No stack trace is returned to a caller. |
| `spring.datasource.url` | `${DB_URL:...}` | The JDBC URL. |
| `spring.datasource.username` | `${DB_USERNAME:seo_v01}` | The database user. |
| `spring.datasource.password` | `${DB_PASSWORD:}` | The database password. |
| `spring.datasource.hikari.maximum-pool-size` | `${DB_POOL_SIZE:10}` | The connection pool size. |
| `spring.datasource.hikari.connection-timeout` | `20000` | The pool checkout timeout. |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Hibernate validates the schema and never changes it. |
| `spring.jpa.open-in-view` | `false` | The persistence context is closed at the service boundary. |
| `spring.flyway.enabled` | `true` | Flyway owns the schema. |
| `spring.flyway.baseline-on-migrate` | `true` | A pre-existing schema is baselined rather than refused. |
| `spring.servlet.multipart.max-file-size` | `20MB` | The upload size cap. |
| `spring.servlet.multipart.max-request-size` | `20MB` | The request size cap. |

## The deployment

### The Dockerfile

Two stages.

**Build stage**, `maven:3.9-eclipse-temurin-21`. The `pom.xml` is copied on its own and
`dependency:go-offline` runs, so the dependency layer is cached separately from the sources
and a source-only change does not re-download the world. Then `src` is copied and
`mvn -B -q -DskipTests package` runs. The artifact is `/build/target/seo-0.1.0.jar`.

**Runtime stage**, `eclipse-temurin:21-jre-jammy`. No compiler, no Maven, no shell history.
A `seo` user and group are created with uid and gid 1001, `/app/data` and `/app/.secrets` are
created and owned by that user, and the jar is copied to `/app/seo.jar`. The container runs as
that user, not as root. The environment sets `SERVER_PORT=8083`,
`SECRET_STORE_PATH=/app/.secrets`, `UPLOAD_DIR=/app/data` and
`JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"`. A healthcheck polls
`/actuator/health` over the loopback interface. The entrypoint is
`exec java $JAVA_OPTS -jar /app/seo.jar`.

### The compose services

| Service | Container | Image | Published | Purpose |
|---|---|---|---|---|
| `db` | `seo-v01-db` | `mysql:8.0` | `127.0.0.1:3307` -> 3306 | The database. The server is started with `--character-set-server=utf8mb4 --collation-server=utf8mb4_0900_ai_ci`. The healthcheck uses `mysqladmin ping`. |
| `app` | `seo-v01-app` | built from the `Dockerfile` | `127.0.0.1:8083` -> 8083 | The application. It waits for `db` to be healthy before it starts. |

The database is published on the loopback interface only, so it is not reachable from outside
the host.

### The volumes

| Volume | Mounted at | What it holds |
|---|---|---|
| `seo-v01-db` | `/var/lib/mysql` | The database files. |
| `seo-v01-secrets` | `/app/.secrets` | The secret store. A credential that is not on a volume is lost when the container is recreated. |
| `seo-v01-data` | `/app/data` | The generated exports. |

## The build

```bash
./mvnw verify
```

`verify` is the gate. It compiles, runs the whole test suite (unit tests plus the H2-backed
integration tests, which Surefire includes explicitly) and applies the Flyway migration to a
clean schema. The suite is hermetic: no network, no live mailbox, no browser.

The repository ships the Maven wrapper. A system Maven is never required.

### The remote build host

Builds run on `tejes@pelican` (Linux, 12 vCPU, Java 21). The wrapper is
`tools/sync-and-build.sh`:

```bash
./tools/sync-and-build.sh            # sync + mvn compile
./tools/sync-and-build.sh test       # sync + mvn test
./tools/sync-and-build.sh verify     # sync + mvn verify
./tools/sync-and-build.sh package    # sync + mvn package
```

It excludes `.git`, `target`, `node_modules`, `.unlazy` and `reference` from the tar stream,
unpacks into `~/seo-rebuild/app` on the host and runs Maven there with
`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`. The host and the directory are overridable
with `SEO_BUILD_HOST` and `SEO_BUILD_DIR`.

**The rule: nothing else on that host may be touched.** `pelican` runs other work. The
sync script only writes to `~/seo-rebuild`, and no command in this repository may start,
stop, reconfigure or restart anything else on that machine, no matter how convenient it
would be. The dedicated MySQL container and the application container for this project are
described in `docs/MAINTENANCE.md`.
