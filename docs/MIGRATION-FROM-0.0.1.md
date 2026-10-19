# Migration from seo-0.0.1

This document is for an operator who knew the original system. It maps every original table
and every original endpoint to its replacement, states the behavioural changes, and gives the
order of operations for moving data across.

The original's behaviour is cited against `docs/reverse-engineering/`, which is the
decompilation record of `seo-0.0.1-SNAPSHOT`. Where this document says "the original did X",
the evidence is a file in `reference/decompiled/` or a section of the reverse-engineering
report.

## Table mapping

| Original table | Replacement | Why |
|---|---|---|
| `login` | `app_user` | The original mixed the account with the licence check and used `IsActive int`, `String` dates and no unique constraint on the username. The replacement separates the account, types the timestamps, adds `failed_login_count` and `locked_until`, and makes the username unique. |
| `client_master` | `client` | The original pointed a foreign key at `client_group.groupCode`, a non-unique, non-primary column, and stored dates as strings. The replacement points `client.group_id` at the primary key of `client_group`, adds the missing index, and uses `DATETIME(6)`. |
| `email_master` | `smtp_account` | The original stored the SMTP password in a plaintext `password` column and returned it in the JSON of `POST /getEmailMaster` and `GET /getEmailMasterById`. The replacement holds a `secret_ref` into the secret store and returns a DTO with a `hasCredential` flag. |
| `gmail_master` | `gmail_account` | The original stored the OAuth client secret, the refresh token and the access token as plaintext columns and returned all three in the JSON of `POST /getAllGmail`. The replacement holds `secret_ref` and `refresh_token_ref` and stores no token. |
| `email_template` | `email_template` | The name is kept because the concept is unchanged. The storage is not: the original had `templateContent VARCHAR(255)` for a full CKEditor HTML document, a free-text `status`, no approver and no version. The replacement has `content_html MEDIUMTEXT`, an enum status, `approved_by`, `approved_at` and `version`. |
| `email_send_job` | `campaign` | The original had a `java.sql.Date date` that discarded the time of day, a nullable `noOfSuccess` and no failure count. The replacement has `started_at` and `finished_at` as `DATETIME(6)`, a non-null success count and a failure count. |
| `email_send_status` | `delivery` | The original wrote one row per recipient with a bare `emailTemplateId` and no foreign key, and the table was never read by any code path. The replacement points `delivery.campaign_id` at `campaign(id)` with `ON DELETE CASCADE`, and adds `provider_message_id` and `error_detail`. |
| `email_queue` | `outbox` | The original had no audit columns at all, a `VARCHAR(255)` error message and no way to claim a row. The replacement adds the four audit columns, `TEXT` error message, `attempt_count` and a conditional claim so a row is never sent twice. |
| `email_extractor` | `extraction_run` | The original had a `java.sql.Date startDate`, a `noOfDomain` that actually counted the domains that yielded an address, and a status that could be left at `IN PROGRESS` forever. The replacement has `started_at` and `finished_at`, a `domain_count`, and a status that always terminates. |
| `email_extractor_data` | `extraction_result` | The original held a bare `emailExtractorId` with no foreign key and no index, so it accumulated orphan rows, and it had no dedupe constraint at all, so a re-run duplicated everything. The replacement has `run_id` with `ON DELETE CASCADE`, an `email_hash` and a unique constraint on `(run_id, email_hash)`. |
| `email_inbox` | `inbox_message` | The original had no `loginId` at all, so the inbox was global, and `content VARCHAR(255)` for a full message body. The replacement points `account_id` at the sending account, has `body_html MEDIUMTEXT`, and a unique constraint on `(account_id, provider_message_id)`. |
| `client_group` | `client_group` | The name is kept. The original had no unique constraint on `groupCode` even though `client_master` used it as a foreign key target, and it stored dates as strings. The replacement adds `uk_client_group_code` and typed timestamps. |
| `role_security` | `app_role` | The original had no unique constraint on the role name and no audit columns. The replacement adds `uk_app_role_code` and typed timestamps, and seeds the four roles the rebuild uses. |
| `login_role_security` | `app_user_role` | The original had no primary key, no index and no timestamp on the join table. The replacement has a composite primary key, an index on `role_id`, two real foreign keys with `ON DELETE CASCADE`, and `granted_at`. |

The seed file `emailsend21.sql` contained 16 tables. Fourteen of them are listed above: the
remaining two had no entity and no code path in the original, so nothing depended on them
and nothing was carried over.

## Endpoint mapping

The original had 48 HTTP mappings across four controllers. Every one of them is listed here.
The column "Rebuild" names the replacement, or states that the endpoint was deliberately not
carried over and why.

### `com.pg.seo.login.LoginController`

| Method | Original path | Rebuild |
|---|---|---|
| GET | `/login` | `GET /login`. The page is owned by the security package now. |
| GET | `/` and `/welcome` | `GET /dashboard`. The original answered `/` with a 403 for every seeded role, because the chain required `hasRole("USER")` and no seeded role was `ROLE_USER` (`reference/decompiled/com/pg/seo/cfg/SecurityConfiguration.java:62`). One entry point now, and it works for every role. |
| GET | `MainDashBoard.htm` (no leading slash) | Not carried over. It was a second route to the same dashboard template, with a path that did not start with a slash. |
| GET | `/sessionExpired` and `/invalidSession` | Not carried over as pages. The application redirects an invalid session to `/login` and the outcome is visible there. |
| GET | `/accessdenied` | Not carried over. The original forwarded to it from the `ExceptionTranslationFilter`, and because `FilterChainProxy` is registered for `REQUEST`, `ASYNC` and `ERROR` only, the forwarded request bypassed the filter chain and the page was reachable anonymously. A refusal is now answered with 403 and nothing else. |

### `com.pg.seo.controller.EmailController`

| Method | Original path | Rebuild |
|---|---|---|
| GET | `/email` | `GET /campaigns`. |
| POST | `/getClientGroup` | `GET /api/groups`. |
| POST | `/getClientMaster` | `GET /api/clients`. |
| POST | `/getEmailMaster` | `GET /api/accounts/smtp`. The original returned the full `EmailMaster` entity including the plaintext SMTP password. The replacement returns a DTO with `hasCredential` and no secret. |
| GET | `/emailTemplate` | `GET /templates`. |
| POST | `/getEmailTemplate` | `GET /api/templates`. |
| POST | `/getEmailTemplateApprove` | `GET /api/templates/pending`. |
| POST | `/getEmailTemplateApproveList` | `GET /api/templates/pending`. The original ran `findAll()` over every tenant's templates and then filtered `status = "N"`, a value nothing in the codebase ever wrote, so the approval queue was always empty. The replacement returns the pending templates of the tenant, and the status is an enum with a real pending state. |
| GET | `/getTemplate` | `GET /api/templates` (list) and `PUT /api/templates/{id}`. The original looked the template up by id with no tenant filter, so any authenticated user could read any template. |
| POST | `/saveEmailTemplate` | `POST /api/templates` and `PUT /api/templates/{id}`. |
| GET | `/removeTemplate` | `DELETE /api/templates/{id}`. The original deleted a template over a GET, which is exempt from CSRF and triggerable from another site with an image tag. |
| POST | `/sendEmail` | `POST /api/campaigns`. The original sent every recipient synchronously on the request thread, with no quota check per account and no outbox. The replacement records the campaign and its delivery rows and checks the quota per account. The job that dispatches a campaign's delivery rows is not implemented yet; the scheduled outbox is the path that sends today. |
| POST | `/emailClientUpload` | `POST /api/clients/import`. The original read column 0 as the name and wrote the same cell into the `webLink` field as well. The replacement writes each field from the column it belongs to. |
| GET | `/emailClient` | `GET /clients`. |
| POST | `/getClientMasterPagination` | `GET /api/clients`. The original computed the page as `start / 100` with a hard-coded page size of 100, ignored the requested length, and returned a hard-coded `recordsTotal` of 100. The replacement returns a real total count. |
| GET | `/clientGroup` | `GET /groups`. |
| GET | `/createClientGroup` | `POST /api/groups`. A state change over GET in the original. |
| GET | `/removeClientGroup` | `DELETE /api/groups/{id}`. |
| GET | `/removeClient` | `DELETE /api/clients/{id}`. The original soft-deleted every client of a group across every tenant, because the lookup had no `loginId`. |
| GET | `/getEmailInbox` | `GET /api/inbox`. The original resolved the mailbox by id with no tenant filter and ignored the `fromDate` and `toDate` arguments entirely. |
| GET | `/emailInbox` | `GET /inbox`. |
| POST | `/getEmailSendJobs` | `GET /api/campaigns`. The original compared a `java.sql.Date` column against an exact millisecond, so the query matched nothing and the list was always empty. |
| GET | `/taskReports` | Not carried over. It returned the hard-coded array `[1000, 3000, 4000, 2000, 500]` and ignored both of its arguments. The dashboard counters come from `GET /api/dashboard`. |
| GET | `/emailMaster` | `GET /accounts/smtp`. |
| POST | `/saveEmail` | `POST /api/accounts/smtp` and `PUT /api/accounts/smtp/{id}`. The password in the request body is write-only: the service puts it in the secret store and drops it. |
| GET | `/getEmailMasterById` | `GET /api/accounts/smtp`. **Not carried over in its original form.** The original returned another tenant's SMTP account, with the plaintext password, to any authenticated caller who guessed an id (`reference/decompiled/com/pg/seo/controller/EmailController.java:324`). The replacement has no endpoint that returns a credential at all. |
| GET | `/removeEmail` | `DELETE /api/accounts/smtp/{id}`. |
| GET | `/getDashboardDbReport` | `GET /api/dashboard`. |
| GET | `/approveTemplate` | `POST /api/templates/{id}/approve`. The original approved any tenant's template over a GET, with no tenant filter. |
| GET | `/approveTemplates` | `GET /templates/approvals`. |
| GET | `/emailExtractor` | `GET /extraction`. |
| POST | `/emailExtractor` | `POST /api/extraction`. The original answered `"result"` on success and `"FAILUER"` on failure, and the service swallowed every exception, so the failure branch was unreachable. The replacement returns the run and its status. |
| POST | `/getEmailExtractor` | `GET /api/extraction`. |
| POST | `/downloadFile` | Not carried over as a file download. The original ignored the `loginId` it had read and exported **every tenant's** scraped addresses, wrote the file to a path built from the raw `fromDate` and `toDate` parameters, and put the unsanitised filename into a response header. The replacement serves the results of one run through `GET /api/extraction/{id}/results`, scoped to the tenant. |
| GET | `/getMonthlyReport` | `GET /api/dashboard`. The original threw a `DateTimeException` for a month above 12. |
| GET | `/getMonthlyEmailReport` | `GET /api/dashboard`. The original summed a nullable `noOfSuccess`, which is an NPE for any job that never completed an iteration. |

### `com.pg.seo.controller.NewEmailController`

| Method | Original path | Rebuild |
|---|---|---|
| GET | `/drafts` | Not carried over. It listed Gmail drafts with no tenant concept at all, because `email_queue` had no tenant column. The Gmail sending path is now driven by the outbox, which is tenant-owned. |
| POST | `/getAllGmail` | `GET /api/accounts/gmail`. **Not carried over in its original form.** The original returned `findAll()` over every `GmailMaster` row, and the entity had no `@JsonIgnore` on any field, so the response carried the OAuth client id, the client secret, the refresh token and the access token for every configured mailbox (`reference/decompiled/com/pg/seo/controller/NewEmailController.java:83`). The replacement returns a DTO with `hasCredential` and no secret. |
| POST | `/send` | `POST /api/outbox`. The original posted the uploaded file to `https://script.google.com/macros/s/YOUR_SCRIPT_ID/exec`, a literal placeholder, with no timeout and no allow-list. That call is gone. |
| GET | `/gmailTemplate` | Not carried over as a page. The Gmail draft workflow is replaced by the outbox, which carries its own body. |
| GET | `/last-24-hours/live` | `GET /api/dashboard`. The original counted messages in `in:sent`, which includes mail sent from other devices, and compared it against a hard-coded 500 or 1500. The quota the application enforces is counted from its own delivery records. |
| GET | `/getScheduledEmail` | `GET /api/outbox`. The original had no tenant filter. |
| - | `@Scheduled(fixedRate = 60000) processScheduledEmails()` | `scheduler/OutboxDispatchJob.dispatchDue`. The original ran the scheduler from a controller method, processed every tenant's queued mail in one batch, and lost the remaining groups when one sender threw (`reference/decompiled/com/pg/seo/controller/NewEmailController.java:131`). |

### `com.pg.seo.controller.EmailSendController`

| Method | Original path | Rebuild |
|---|---|---|
| POST | `/sendData` | Not carried over. It parsed the uploaded workbook and forwarded the rows to `https://script.google.com/macros/s/YOUR_SCRIPT_ID/exec` with a fresh `RestTemplate` that had no timeout and no allow-list. The endpoint was non-functional as shipped and it posted user data to an external host. |

### Two security-config matchers that pointed at nothing

The original filter chain permitted `/api/v1/getBarcode4j` and `/word`
(`reference/decompiled/com/pg/seo/cfg/SecurityConfiguration.java:62`). No controller and no
route existed for either. They were `permitAll` holes for URLs that 404. Neither appears in
the rebuild.

## Behavioural fixes

Each row states what the original did, with the evidence, and what the rebuild does instead.

### Tenant isolation

| Original | Rebuild |
|---|---|
| The tenant lived in a `loginId` session attribute read out of `HttpSession` inside every controller, and was passed to services that ignored it in eleven places. `removeClient` looked a group up with `findByGroupCode(groupCode)` and no tenant, so one tenant could soft-delete another tenant's clients (`reference/decompiled/com/pg/seo/service/EmailService.java:463`). `getTemplate`, `approveTemplate`, `getEmailMasterById` and `getEmailInbox` had no tenant filter at all. `getEmailTemplateApproveList` ran `findAll()`. `downloadFile` exported every tenant's scraped addresses. The whole `NewEmailController` surface had no tenant concept. | The tenant is the authenticated user. The owner id is read from the security context, passed explicitly into every service method, and filtered again in the repository. `src/test/java/com/pg/seo/integration/TenantIsolationIT.java` asserts the isolation across every repository method and every mutating service. |

### State changes over GET

| Original | Rebuild |
|---|---|
| Six endpoints mutated state over `@GetMapping`: `/removeTemplate`, `/createClientGroup`, `/removeClientGroup`, `/removeClient`, `/removeEmail`, `/approveTemplate` (`reference/decompiled/com/pg/seo/controller/EmailController.java:162,225,241,257,330,352`). A GET is exempt from CSRF, so `<img src=".../removeClient?groupCode=X">` deleted a client group from any other site. | Every mutation is a `POST`, `PUT` or `DELETE`, and therefore CSRF-protected. No `@GetMapping` in the rebuild changes state. |

### CSRF

| Original | Rebuild |
|---|---|
| CSRF was never configured, so it was at the Spring default and enabled. But six state-changing GETs were exempt from it, and every legitimate JSON or multipart POST returned 403 unless the client sent the token. | CSRF stays at the Spring default and stays enabled. Every page declares `<meta name="_csrf" th:content="${_csrf.token}">` and the front end sends the token as `X-CSRF-TOKEN`. A missing token is answered with a redirect to the login page, because Spring Security reads a missing token as a lost session. A wrong token, or a caller with no role, is answered with 403. |

### Credential storage and exposure

| Original | Rebuild |
|---|---|
| `email_master.password` held 41 SMTP application passwords in plaintext. `gmail_master.secret`, `.refreshToken` and `.accessToken` held OAuth material in plaintext. `POST /getEmailMaster`, `GET /getEmailMasterById` and `POST /getAllGmail` returned those columns to the browser, because no entity field carried `@JsonIgnore`. `hibernate.show_sql = "true"` printed the inserts, so the SMTP passwords also landed in stdout (`reference/decompiled/com/pg/seo/cfg/JpaConfigurationMySql.java:80`). | No credential is stored in the database. The entity holds a `secret_ref` into the secret store, the adapter resolves the value at call time and drops it, and no DTO carries a credential. `seo.jpa.show-sql` is `false`. The original's credentials must be rotated; see `docs/SECRET-HARVEST.md`. |

### The licence and the MAC lock

| Original | Rebuild |
|---|---|
| Every login attempt read the host's MAC address and compared it with the `mac` property, and decoded an HS256 licence JWT **without verifying the signature**, then checked its expiry. Both failures raised the same `UsernameNotFoundException`, which Spring turned into the generic "Your username and password is invalid." The licence had expired on 2025-09-18, so the original could not authenticate anyone on any machine. The MAC was printed to stdout on every attempt (`reference/decompiled/com/pg/seo/login/LoginDetailsServiceImpl.java:65`). | No licence, no activation, no machine fingerprinting. There is no `NetworkInterface` or `getHardwareAddress` call in the codebase, and `tools/verify/code-hygiene.mjs` fails the build if one reappears. Authentication and role-based authorization stay: they are not licensing. |

### The daily limit that was actually monthly

| Original | Rebuild |
|---|---|
| `emailSend` summed `email_send_job.no_of_email` over the first and last day of the current calendar month and refused to send when the sum exceeded a hard-coded 50 000, returning the string `"Daily Limit Reached "` (`reference/decompiled/com/pg/seo/service/EmailService.java:114`). | `seo.quota.daily-send-limit`, `hourly-send-limit` and `monthly-send-limit`, per tenant and per sending account, counted from the delivery records in calendar windows in UTC. The daily limit is a daily limit. |

### The broken pagination

| Original | Rebuild |
|---|---|
| `PageRequest.of((int)(pagingRequest.getStart() / 100), (int)100)` hard-coded the page size at 100, ignored the requested length, and the controller set `recordsTotal` to the literal `100L`, so the total was never the number of rows (`reference/decompiled/com/pg/seo/controller/EmailController.java:271`). | `GET /api/clients` returns a `PagedResponse` with a real total count and the requested page size. |

### The empty IMAP search term

| Original | Rebuild |
|---|---|
| The inbox job searched with `ReceivedDateTerm(LE, now) AND ReceivedDateTerm(GE, now)`, which matches only messages received at exactly that moment, so it fetched nothing in practice. The intent was "since the start of today" (`reference/decompiled/com/pg/seo/service/EmailInboxService.java:85`). | `scheduler/InboxPollJob` polls since the last poll and identifies a message by the id the provider gave it, so a repeated poll stores it once. |

### The unbounded extraction fan-out

| Original | Rebuild |
|---|---|
| `extract()` submitted one `CompletableFuture` per spreadsheet row to the common pool with a 360-minute timeout, then blocked the async thread until all of them finished. The inner pool was `newFixedThreadPool(cores * 2)` with an unbounded queue. The dedupe map was a process-wide `ConcurrentHashMap` that was never cleared, so a second run over the same domain returned nothing at all (`reference/decompiled/com/pg/seo/service/NewEmailExtractorService.java:50`). | A fixed pool of 8 threads with a bounded queue of 500, the domains submitted in chunks, a per-domain budget, a cap of 5 000 domains per run, and a dedupe scoped to the run with a unique constraint in the database. |

### The SSRF in the extraction

| Original | Rebuild |
|---|---|
| `Jsoup.connect("https://" + row.getCell(0).getStringCellValue())` fetched a URL built straight from a spreadsheet cell, with no allow-list, no scheme check and no private-range check, so a cell could reach the loopback interface or the link-local metadata address. The dead variant set no timeout at all (`reference/decompiled/com/pg/seo/service/EmailService.java:519`, `NewEmailExtractorService.java:64`). | `extraction/UrlAllowList` checks every URL before the request: the scheme must be `http` or `https`, the host must be on the configured suffix list unless arbitrary hosts are allowed, and **every** address the host resolves to must be public. The private ranges are blocked unconditionally and the block cannot be switched off. Redirects are followed at most twice and each hop is re-validated. |

### The shared `SimpleDateFormat`

| Original | Rebuild |
|---|---|
| `EmailController.simpDate`, `EmailController.sqlDate`, `EmailService.sdfDateTime`, `sdfDate`, `sdfSqlDate` and `sdfRegularDateTime` were non-thread-safe `SimpleDateFormat` instance fields on singletons, shared between the request thread and the `@Async` thread. Under concurrency they produced corrupt date strings (`reference/decompiled/com/pg/seo/service/NewEmailExtractorService.java:45`). | `java.time` throughout. Every timestamp is an `Instant` mapped to `DATETIME(6)`. There is no `SimpleDateFormat` in the codebase. |

### The single-threaded scheduler

| Original | Rebuild |
|---|---|
| `cfg/SchedulerConfig` declared `new ConcurrentTaskScheduler()`, whose default pool size is 1. The 11:00 IMAP job and the 60-second Gmail job shared that one thread, so a stalled IMAP connection with no socket timeout delayed every other job (`reference/decompiled/com/pg/seo/cfg/SchedulerConfig.java:16`). | The scheduler pool has more than one thread and an error handler, so one failing job never stops the others and never stops the schedule. Each job claims its work with a conditional status update, so a job that runs twice does the work once. |

### The unbounded SMTP timeouts

| Original | Rebuild |
|---|---|
| The `JavaMailSenderImpl` was built without `mail.smtp.connectiontimeout`, `mail.smtp.timeout` or `mail.smtp.starttls.required`, so the socket timeout was infinite. A new sender, and therefore a new SMTP connection, was built per recipient (`reference/decompiled/com/pg/seo/service/EmailService.java:82`). | `seo.mail.connect-timeout`, `read-timeout` and `write-timeout`, default 15 s, 30 s and 30 s. One `MimeMessage` per recipient. |

### The `findAll()` endpoints

| Original | Rebuild |
|---|---|
| `getEmailTemplateApproveList` and `getAllGmail` both ran `findAll()`. The `email_extractor_data` table held 3 583 scraped addresses with no index on the foreign key and no dedupe constraint, and `getEmailInbox` and `getEmailSendJobs` loaded unbounded result sets with no limit. | `findAll()` is forbidden on a tenant-owned table. Every repository method states its filter in its name. Every list endpoint is paged and the total is real. |

### The dead code

| Original | Rebuild |
|---|---|
| `LogoutController` was an empty class with no annotations. `SecurityService` and `SecurityServiceImpl` had no caller. `LoginServiceImpl` was autowired in two places and never used, and its `save()` would have granted the first role in the table to every new user. `SecurityConfiguration.passwordencoder()` and `WebMvcConfigruation.taskExecutor()` were missing `@Bean` and so never existed. `SecurityConfiguration.logoutSuccessHandler()` was a `LogoutHandler` bean that was never wired, and the handler it pointed at was a no-op. `GmailServiceFactory`, `EmailExtractionService`, `PuppeteerRunner` and `AmazonScraperService` were never called. `EmailService.extract` and `EmailService.extracted` were superseded by a second implementation. `EmailController.simpDate` was unused (`reference/decompiled/com/pg/seo/login/LoginServiceImpl.java:41`). | Every class is reachable. The configuration rule is "every executor method has `@Bean`", because a method without it silently never exists. The test context loads and fails if any bean is ambiguous, missing or unused in a way that matters. |

### The missing `SessionRegistry`

| Original | Rebuild |
|---|---|
| The chain declared `maximumSessions(1)` but there was no `SessionRegistry` bean and no `HttpSessionEventPublisher` bean anywhere. Expired sessions were never evicted from the registry, so it grew without bound and users were spuriously logged out as the max-session count was reached (`reference/decompiled/com/pg/seo/cfg/SecurityConfiguration.java:66`). | `SecurityConfig` declares both a `SessionRegistry` and an `HttpSessionEventPublisher`, so expired sessions are evicted. |

### Other corrections worth naming

| Original | Rebuild |
|---|---|
| The subject line was the template *name* (`mail.setSubject(emailTemplate.getTemplateName())`), and the template name was free text, so it went into a header unfiltered. | The subject is a separate field of `email_template`, and it is not derived from a name. |
| One `MimeMessage` and one `MimeMessageHelper` were reused across every recipient of a campaign, so a failed send left partially applied state that the next iteration inherited. | One `MimeMessage` per recipient. |
| The scraper sidecar bound every interface on port 3000 with `Access-Control-Allow-Origin: *` and no authentication, and its only check was `url.startsWith("https://www.amazon.")`, which `https://www.amazon.evil.com/` passes. | The sidecar is reached over loopback only, with a request timeout, and the keyword travels in a query parameter. |
| The Node payload used `sellerDetails` while the Java model expected `sellerInfo`, and the payload never carried a price, so both were always null. | The contract is fixed on both sides. |
| `addEmailClient` wrote the same cell into the name and the web link, and dereferenced the second column unguarded, so an empty column B was an NPE that was swallowed into an empty status string. | Each field is read from the column it belongs to, and a malformed row is a reported error. |
| The extraction wrote one row per address with a separate `save()`, never in a batch. | Batches of `seo.extraction.write-batch-size`. |
| `hibernate.hbm2ddl.auto = "validate"` with no Flyway and no Liquibase, so the schema was hand-created and drift was only ever detected by a boot-time crash. | Flyway owns the schema. `V1__baseline.sql` is the baseline. Hibernate only validates. |
| `spring.sql.init.continue-on-error = true` masked a failed schema initialisation. | There is no schema initialisation outside Flyway. |
| `app.upload.path = C:\deployment`, a Windows absolute path used by bare string concatenation with no separator. | `seo.storage.upload-dir`, a configured directory, and no path is ever built from a user-supplied value. |
| The mail quota estimate counted `in:sent` messages, which includes mail sent from other devices, against a hard-coded 500 or 1500. | The quota is counted from the application's own delivery records. |

## Moving the data across

The original's data cannot be loaded with the original's own schema, and it cannot be loaded
into the rebuild's schema either. The reasons are structural:

- The original's columns are camelCase with `VARCHAR` dates and `java.sql.Date` columns.
  The rebuild's columns are snake_case with `DATETIME(6)`.
- The original's soft delete is `isActive int` plus a Hibernate class filter. The rebuild's
  is `deleted_at DATETIME(6) NULL`.
- The original has no unique constraint on anything, so a dump may contain duplicate
  usernames, duplicate group codes and duplicate account addresses. The rebuild's schema
  rejects those.

### What cannot be carried over

| Data | Why | What to do |
|---|---|---|
| `email_master.password`, the 41 SMTP application passwords | Plaintext credentials. The rebuild's `smtp_account` has no password column, and the values are also exposed to anyone who has the original database. | Re-enter them. The password entered when the account is created goes into the secret store. The original values must also be rotated, because they were in a shipped artifact (`docs/SECRET-HARVEST.md`). |
| `gmail_master.secret`, `.refreshToken`, `.accessToken` | Plaintext OAuth material, and the refresh token grants permanent read and send access to the mailbox. | Re-enter the OAuth client secret and re-authorise the mailbox to obtain a fresh refresh token. The exposed refresh tokens must be revoked. |
| The `license` JWT and the `mac` value | There is no licence and no machine lock in the rebuild, and neither column has a home. | Dropped entirely. |
| The `wordpress.api.token` | The WordPress integration the token was for does not exist in the original either: no decompiled class consumed the property. | Dropped. Rotate it. |
| `email_extractor_data` duplicates | The rebuild enforces a unique `(run_id, email_hash)`. | Deduplicate on the way in, or let the first insert win and skip the rest. |
| `client_group.groupCode` duplicates | The rebuild enforces `uk_client_group_code`. | Deduplicate before the import, and merge the members of the duplicates into one group. |

### Order of operations

1. **Rotate the exposed credentials first.** Do this before anything else. The whole list and
   the order are in `docs/SECRET-HARVEST.md`. Migrating data that carries live credentials
   into a new system spreads them further.
2. **Stand up the rebuild empty.** `docker compose up -d --build`, let Flyway apply
   `V1__baseline.sql`, then stop the application again so nothing writes while the import
   runs.
3. **Import the accounts.** `app_user`, then `app_role`, then `app_user_role`. The rebuild
   seeds the four roles and one administrator, so map the original's `ROLE_ADMIN`,
   `ROLE_EMP`, `ROLE_MANG` and `ROLE_HR` onto them: `ROLE_ADMIN` to `ROLE_ADMIN`,
   `ROLE_MANG` to `ROLE_MANAGER`, `ROLE_EMP` to `ROLE_OPERATOR`, `ROLE_HR` to `ROLE_VIEWER`.
   The original's role names are data, not code, and the rebuild's role codes are a
   different set. Decide the mapping and write it down before the import.
4. **Re-enter the passwords.** The original's `login.Password` holds BCrypt hashes at cost
   12. The rebuild verifies against cost 12, so the hashes can be copied as they are. The
   original seed reused one hash across two accounts, and two of the three accounts are
   known to be exposed, so a password reset for every migrated account is the safer route.
   The rebuild's own account table adds a status, a failed-login counter and a lock window
   that the original had no equivalent for.
5. **Import the client data.** `client_group` first, then `client`. The original's
   `client_master.groupCode` becomes `client.group_id`, resolved against the group that was
   imported in step 5. Fix the duplicates that the new unique constraint would reject.
   `isActive = 0` rows become `deleted_at` set to the row's own modification time.
6. **Import the templates.** `email_template`. `content_html` is `MEDIUMTEXT` in the
   rebuild, so the CKEditor HTML survives. The original's `status` was free text with a
   meaningful value of `A` for approved and `N` for pending, where nothing ever wrote `N`;
   map `A` to `APPROVED` and anything else to `DRAFT`. The rebuild's `version` starts at 1
   and the rebuild's `approved_by` and `approved_at` are null for a template that was never
   approved inside the rebuild.
7. **Re-create the sending accounts.** `smtp_account` and `gmail_account`, with the
   credential re-entered through the application so it lands in the secret store. The
   account rows themselves (host, port, protocol, auth method, username) can be imported
   directly, but `secret_ref` is only meaningful once the secret is in the store.
8. **Import the send history.** `email_send_job` becomes `campaign` and `email_send_status`
   becomes `delivery`. The original's `email_send_status.emailTemplateId` points at a
   template, while the rebuild's `delivery.campaign_id` points at a campaign, so the
   per-recipient rows need the campaign resolved from the original's `email_send_job`
   grouping rather than from their own columns. The original's `email_send_status` table was
   never read by any code path, so it may be incomplete.
9. **Import the outbox.** `email_queue` becomes `outbox`. The original's
   `scheduledTime` was the one proper timestamp in the original schema, so it maps
   directly. Anything still `PENDING` at cutover is worth deciding about explicitly: it
   will be picked up by the dispatcher and sent.
10. **Import the extraction history.** `email_extractor` becomes `extraction_run` and
    `email_extractor_data` becomes `extraction_result`. The original's
    `email_extractor_data.emailExtractorId` becomes `run_id`. The `email_hash` column is
    computed from the normalised address, and the unique constraint on `(run_id,
    email_hash)` will reject a duplicate that the original happily stored. The 3 583
    addresses in the original seed are scraped contact data, not the application's own
    records, so they carry a data-protection obligation; decide whether to import them at
    all.
11. **Import the inbox.** `email_inbox` becomes `inbox_message`. The original's
    `email_inbox` had no `loginId` at all, so it was global and there is no tenant to
    attribute the messages to. `inbox_message.account_id` points at a sending account.
    Messages whose account cannot be resolved have to be attributed to a tenant by hand.
12. **Verify before you open it up.** Check that the row counts match what you expect per
    table, that no user can see another user's clients, groups, templates, campaigns,
    deliveries, outbox rows, extraction results or inbox messages, and that the delivery
    counts add up against the campaign counts.
13. **Start the application.** The dispatcher will pick up any outbox row that is still due.
    Decide whether that is what you want before you start it.

### What is not worth migrating

- The original's `email_send_status` table was write-only. Nothing ever read it.
- The original's `email_inbox` rows are inbound messages that the original never fetched
  successfully, because the IMAP search term matched nothing. In practice the table is
  either empty or holds whatever a manual insert put there.
- The original's `/taskReports` and `/getMonthlyReport` numbers were hard-coded or derived
  from nullable columns, and the dashboard in the rebuild computes its own counters.
