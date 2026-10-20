<task id="ses_eaab35070ffeTL54FwlzFnabKJ" state="completed">
<task_result>
# Decompiled Reference Report — `com.pg.seo` (Spring Boot 3.3.0 / Java 17)

Source of truth: `/Users/saiduttaabhishekdash/seo-0.1.0/reference/decompiled/com/pg/seo/**` (CFR 0.152).
Cross-checked against the original artifact `/Users/saiduttaabhishekdash/seo-0.0.1-SNAPSHOT/seo-0.0.1-SNAPSHOT.jar` (BOOT-INF/classes + BOOT-INF/lib) for resources/config/libs that are not in the decompile.

## 0. Runtime context (from the JAR, needed to read the code correctly)

`BOOT-INF/classes/application.properties`:

```properties
server.port=8083
app.datasource.mysql.username=root
app.datasource.mysql.password=[redacted - see SECRETS.txt]
app.datasource.mysql.driver-class-name=com.mysql.jdbc.Driver
app.datasource.mysql.url=jdbc:mysql://localhost:3306/emailsender
license=eyJhbGciOiJIUzI1NiJ9....   # HS256, signature never verified
mac=[redacted - see SECRETS.txt]               # machine lock
spring.jpa.hibernate.use-new-id-generator-mappings=false
spring.sql.init.continue-on-error=true
spring.servlet.multipart.max-file-size=128MB
spring.servlet.multipart.max-request-size=128MB
app.upload.path=C:\deployment
wordpress.api.token=$l&x6Rh4%rloALGqwvN^lWDjFZ4mr%PuU4KsuKTus%idKgAIutmcn4yCJUjeo7vt
```

Library versions in `BOOT-INF/lib`: `spring-boot-3.3.0`, `spring-context-support-6.1.8` (Spring 6.1.8), `HikariCP-5.1.0`, `mysql-connector-j-8.0.31`, `jakarta.mail-2.0.3`, `jsoup-1.18.1`, `poi-ooxml-5.2.5`, `google-api-services-gmail-v1-rev110-1.25.0`.

`com.pg.seo.EmailSenderApplication` (bootstrap):
```java
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class EmailSenderApplication { ... }
```

---

## 1. `service/EmailService.java`
Path: `/Users/saiduttaabhishekdash/seo-0.1.0/reference/decompiled/com/pg/seo/service/EmailService.java` (627 lines)

### 1.1 Purpose and public API
`@Service`, no `@Transactional`, no interface. Handles SMTP campaigns, templates, client groups, Excel upload, email extraction (legacy), Excel report.

Public method signatures (exact):
```java
public String emailSend(EmailSendRequest emailSendRequest, String userName, Long loginId) throws MessagingException
public List<ClientGroupDto> getClientGroup(Long loginId)
public ClientMasterResponse getClientMaster(Long loginId)
public ClientMasterResponse getClientMasterPagination(Long loginId, PagingRequest pagingRequest)
public List<EmailMaster> getEmailMasters(Long loginId)
public List<EmailTemplate> getEmailTemplates(Long loginId)
public EmailTemplate getEmailTemplate(Long emailTemplateId)
public Long removeEmailTemplate(Long emailTemplateId, String userName)
public String saveEmailTemplate(String templateName, String templateContent, String userName, Long loginId)
public String addEmailClient(MultipartFile file, String groupCode, String userName, Long loginId)
public Long createClientGroup(String groupCode, String groupDesc, String userName, Long loginId)
public Long removeClientGroup(String groupCode, String userName, Long loginId)
public Integer removeClient(String groupCode, String userName, Long loginId)
public List<EmailSendJob> getEmailSendJobs(Long loginId)
public Long saveEmailMaster(EmailMasterDto emailMasterDto, String userName, Long loginId)
public EmailMaster getEmailMaster(Long emailMasterId)
public Long rmoveEmailMaster(Long emailMasterId, String userName)   // sic: "rmove"
public DashboardDbReport getDashboardDbReport(Long loginId)
public List<EmailTemplate> getEmailTemplatesForApprove()
public Long approveTemplate(String userName, Long emailTemplateId)
public void extract(InputStream file, Long loginId) throws ExecutionException, InterruptedException, TimeoutException  // @Async
public ConcurrentHashMap<String, String> extracted(Row row, Long emailExtractorId)
public String generateExcel(String startDate, String endDate) throws ParseException
public List<EmailExtractor> getEmailExtractorList(Long loginId)
public EmailExtractor getEmailExtractor(Long emailExtractorId, Long loginId)
public List<Long> getMonthlyReport(int month, Long loginId)
public List<Long> getMonthlyEmailReport(int month, Long loginId)
```
Private helper: `private String emailSender(JavaMailSenderImpl, MimeMessage, MimeMessageHelper, Mail) throws MessagingException`.

Injected state: 8 repositories, `SpringTemplateEngine templateEngine`, `@Value("${app.upload.path}") public String uploadpath`.
Non-`final` instance `SimpleDateFormat` fields (not thread-safe): `sdfDateTime="yyyy-MM-dd HH:mm:ss"`, `sdfDate="dd-MM-yyyy"`, `sdfSqlDate="yyyy-MM-dd"`, `sdfRegularDateTime="dd-MM-yyyy HH:mm"`.

### 1.2 External systems
- **SMTP** — a *new* `JavaMailSenderImpl` is constructed per campaign, configured from the `email_master` row of `fromEmail`:
  ```java
  props.put("mail.smtp.starttls.enable", emailMaster.getEnable());
  props.put("mail.smtp.auth", emailMaster.getAuth());
  props.put("mail.transport.protocol", emailMaster.getProtocol());
  mailSender.setHost(emailMaster.getHost());
  mailSender.setPort(emailMaster.getPort().intValue());
  mailSender.setUsername(emailMaster.getUsername());
  mailSender.setPassword(emailMaster.getPassword());
  ```
  No `mail.smtp.connectiontimeout` / `mail.smtp.timeout` / `mail.smtp.starttls.required` → default infinite socket timeout.
- **Thymeleaf** — `templateEngine.process("mailtemplate", context)`; template = `BOOT-INF/classes/templates/mailtemplate.html`. Context vars: `name`, `webLink`, `content` (content is rendered with `th:utext` → raw HTML, `webLink` with `th:text` → escaped).
- **MySQL** — via Spring Data JPA repositories; native SQL used (see §12).
- **Outbound HTTPS (JSoup)** — `Jsoup.connect("https://" + row.getCell(0).getStringCellValue()).get()` in `extracted()`. Default JSoup timeout 30 s, no explicit timeout set.
- **Filesystem** — `generateExcel` writes to `app.upload.path` (`C:\deployment` in the shipped config): `new FileOutputStream(new File(this.uploadpath + filename))`.
- No WordPress call from this class (the `wordpress.api.token` config property is not consumed by any decompiled class).

### 1.3 Threading / async
- `@Async` on `extract(InputStream, Long)`. Default executor = Spring Boot's `applicationTaskExecutor`/`taskExecutor` auto-config (core 8, unbounded queue). `WebMvcConfigruation.taskExecutor()` is **not** a `@Bean`, so its `GithubLookup-` pool never exists.
- Inside `extract`, per-row work is fanned out on `CompletableFuture.supplyAsync(...)` → **ForkJoinPool.commonPool()** (not the injected executor):
  ```java
  CompletableFuture requestCompletableFuture = CompletableFuture.supplyAsync(() -> this.extracted(row, emailExtractorId))
      .completeOnTimeout(new ConcurrentHashMap(), 360L, TimeUnit.MINUTES);
  ```
  Timeout = **360 minutes**; then `CompletableFuture.allOf(...).thenApply(...).get()` blocks the async thread until all rows finish.
- `emailSend` is **fully synchronous on the request thread**.
- `SimpleDateFormat` instance fields are shared between the request thread and the `@Async` thread → data race (corrupt date strings under concurrency).

### 1.4 Retry / batching / rate limit / concurrency control
- **No retry anywhere** in this class. A failed send is recorded as `FAILURE` and the recipient is skipped.
- **No batching**: one `MimeMessage` reused, one SMTP `send()` per recipient, one `email_send_status` INSERT per recipient, and 2 extra SELECTs + 1 UPDATE of `email_send_job` per recipient.
- **Rate limit**: a single cap, checked *before* sending:
  ```java
  List<EmailSendJob> emailSendJobList = this.emailSendJobRepository.findByLoginIdMonth(loginId, Date.valueOf(startDate), Date.valueOf(lastDate));
  Integer emailSendJobCount = emailSendJobList.stream().map(EmailSendJob::getNoOfEmail).reduce(0, Integer::sum);
  if (emailSendJobCount > 50000) { return "Daily Limit Reached "; }
  ```
  It is a **monthly** cap of 50 000 emails (window = first..last day of current month) mislabelled "Daily".
- No delay/sleep between sends; no connection pool (new `JavaMailSenderImpl` ⇒ new `Session` ⇒ new SMTP connection per recipient).

### 1.5 Error handling
- `emailSender` catches `Exception`, calls `e.printStackTrace()`, returns `"FAILURE"`; per-recipient failure is non-fatal and is written to `email_send_status.status` as the literal string `"FAILURE"` (success = `"SUCCESS"`).
- `addEmailClient` catches `Exception` → `printStackTrace()` → returns `""` (empty string, not "FAILURE").
- `generateExcel` catches `Exception` → `printStackTrace()` → still returns the filename even if the file was never written.
- `extracted` has a **completely empty catch block** (`catch (Exception exception) {}`) around the whole scrape, and another empty catch around the per-email DB save.
- `extract` catches `Exception` → `printStackTrace()`; if it throws before the final status update, the `email_extractor` row stays `"IN PROGRESS"` forever.
- `emailSend` declares `throws MessagingException` and does **not** catch: an exception from `emailMasterRepository.findByEmail`, `emailTemplateRepository.findByEmailTemplateId`, `clientMasterRepository.findByClientMasterId`, `Long.parseLong(...)`, or the final `findById(...).get()` propagates to the controller → HTTP 500, and the `email_send_job` row is left at status `"In Progress"`.

### 1.6 Security-relevant behaviour
- SMTP password and `email_master` rows are read in plaintext from the DB; `saveEmailMaster` stores the password **unencrypted** in `email_master.password`.
- `getEmailMasters(loginId)` / `getEmailMaster(id)` return the full `EmailMaster` entity, **including the plaintext SMTP password**, straight to the browser as JSON (`/getEmailMaster`, `/getEmailMasterById` in `EmailController`).
- SSRF: `extracted()` issues `GET https://<cell value>` where the cell comes from an uploaded Excel file — arbitrary host, no allow-list, no redirect limits.
- `th:utext="${content}"` renders operator-supplied template HTML unescaped (stored XSS in the mail body is expected/by design; but it also means a compromised template injects arbitrary HTML into every recipient's mail).
- No secrets are written to logs; failures are logged only via `printStackTrace()` (leaks stack traces + message content to stdout, and `MessagingException` messages can include the SMTP server response).

### 1.7 Bugs / race conditions / leaks
1. **Reused `MimeMessage` across recipients** — `message`/`helper` are created once outside the loop. Verified against spring-context-support 6.1.8 bytecode: `MimeMessageHelper.setTo(String)` → `setRecipient` (**replace**), `setFrom` → `setFrom` (replace), `setText(text,true)` → `mimeMessage.setContent(html,"text/html;charset=UTF-8")` (no multipart is created even though the helper was built with `new MimeMessageHelper(message, 3, "UTF-8")`; mode 3 = ALTERNATIVE is effectively ignored, so mails are HTML-only with no plain-text part). Net effect: no cross-recipient leakage, but a single shared mutable object — a failed send leaves partially-applied state that the next iteration reuses.
2. **Subject is the template *name***, not a subject field: `mail.setSubject(emailTemplate.getTemplateName())`.
3. `addEmailClient` writes **the same cell 0 value into three fields** — `name`, `webLink` and (implicitly) the group lookup all use `row.getCell(0)`; `email` comes from `row.getCell(1)`. Null check is on cell 0 but cell 1 is dereferenced unguarded → NPE if column B is empty.
4. `removeClient(groupCode, ...)` and `removeClientGroup(...)` use `clientMasterRepository.findByGroupCode(groupCode)` which has **no `loginId` filter** → one tenant can soft-delete another tenant's clients sharing a group code. `removeClient` returns the count of *all* clients in the group.
5. `getClientMasterPagination` hard-codes page size 100 and ignores `PagingRequest.length`: `PageRequest.of((int)(pagingRequest.getStart() / 100), (int)100)`; `start=99` → page 0. `EmailController` then hard-codes `page.setRecordsTotal(100L)`.
6. `generateExcel` filename bug: `startDate + "_" + endDate + "_" + emailExtractorDatalistNew.size() + "1.xlsx"` → e.g. `2024-01-01_2024-01-31_35831.xlsx` (stray literal `1`).
7. `generateExcel` dedupe is by **domain only** (`TreeSet` with `Comparator.comparing(EmailExtractorData::getDomain)`) → only one email per domain survives in the export.
8. `getMonthlyEmailReport` sums `EmailSendJob::getNoOfSuccess` — that column is `null` for jobs that never completed an iteration → **NPE** in `Integer::sum`.
9. `emailSendJob.setNoOfSuccess(noOfSuccess)` then `findById(...).get()` per recipient → 3 DB round-trips per recipient; `Optional.get()` without `isPresent()` → `NoSuchElementException` if the row vanished.
10. NPE risks on: `emailSendRequest.getEmailType()` (null), `emailSendRequest.getClient()` (null → `.length` NPE), `Long.parseLong(email)` (`NumberFormatException` if the client array holds non-numeric ids), `clientMaster` null from `findByClientMasterId`, `emailMaster` null from `findByEmail`, `emailTemplate` null, `emailMaster.getPort()` null.
11. `extract()` closes the workbook (`wb.close()`) **inside** the try while async tasks still hold `Row` objects from that workbook → POI "workbook already closed" errors for later rows; swallowed by the empty catch in `extracted()` → silent data loss.
12. `extract()` submits an unbounded number of `supplyAsync` tasks (one per Excel row) to the common pool → task explosion for large files.
13. `extracted()` saves **one row per email address** (no `saveAll` batch) and only keeps the *first* match per domain in the returned map, but the DB receives every match.
14. `emailSendStatus.setStatus("S")` is set then immediately overwritten with `"SUCCESS"`/`"FAILURE"` — the `"S"` literal is dead.

---

## 2. `service/GmailService.java`
Path: `/Users/saiduttaabhishekdash/seo-0.1.0/reference/decompiled/com/pg/seo/service/GmailService.java` (172 lines)

### 2.1 Purpose and public API
`@Service`. Wraps the Gmail REST API (send a *draft* with `{{Domain}}` substituted, list drafts, count sent mail in last 24 h).

```java
public Gmail createGmailService(String email) throws Exception
public Map<String, String> getAllDraftSubjectsWithIds(String email) throws Exception
public void sendDraftEmailById(String fromEmail, String toEmail, String draftId, String domainValue) throws Exception
public Map<String, Object> getEmailSendStats(String fromEmail) throws Exception
public List<GmailMaster> getAllGmail()
private int determineQuotaLimit(String email)
```
Constants: `PERSONAL_GMAIL_LIMIT = 500`, `WORKSPACE_GMAIL_LIMIT = 1500` (both **unused**; `determineQuotaLimit` hard-codes the literals), `JSON_FACTORY = GsonFactory.getDefaultInstance()`.

### 2.2 External systems (Gmail API)
Google API client `google-api-services-gmail-v1-rev110-1.25.0`; base `https://gmail.googleapis.com/gmail/v1/users/me/...`, token endpoint `https://oauth2.googleapis.com/token`.

| Java call | HTTP |
|---|---|
| `gmail.users().drafts().list("me").execute()` | `GET /gmail/v1/users/me/drafts` |
| `gmail.users().drafts().get("me", draftId).execute()` | `GET /gmail/v1/users/me/drafts/{id}` |
| `gmail.users().drafts().get("me", draftId).setFormat("raw").execute()` | `GET /gmail/v1/users/me/drafts/{id}?format=raw` |
| `gmail.users().messages().list("me").setQ(query).setMaxResults(500L).setPageToken(tok).execute()` | `GET /gmail/v1/users/me/messages?q=...&maxResults=500&pageToken=...` |
| `gmail.users().messages().send("me", newMsg).execute()` | `POST /gmail/v1/users/me/messages/send` (body `{"raw":"<base64url>"}`) |
| `credential.refreshToken()` | `POST https://oauth2.googleapis.com/token` (refresh_token grant) |

Application name registered with the API: `"MAIL ELEXYT"` (typo, in both `GmailService` and `cfg/GmailServiceFactory`).
`getEmailSendStats` query string: `"in:sent after:" + Instant.now().minus(24, ChronoUnit.HOURS).getEpochSecond()`.

### 2.3 Threading
None. No `@Async` / `@Scheduled`. `GmailService` is called from `NewEmailController` (request thread) and from the scheduled `processScheduledEmails` (scheduler thread → `commonEmailExecutor` threads).

### 2.4 Batching / limits
- `getEmailSendStats` pages with `setMaxResults(500L)` and follows `nextPageToken` until null; counts messages returned.
- Quota model (estimate only, not enforced):
  ```java
  private int determineQuotaLimit(String email) {
      if (email.toLowerCase().endsWith("@gmail.com")) return 500;   // personal
      return 1500;                                                // workspace
  }
  int remainingQuota = Math.max(0, dailyLimit - totalMessages);
  ```
  Returned map keys: `fromEmail`, `emailsSentInLast24Hours`, `limitEstimate`, `remainingQuotaEstimate`.
- `sendDraftEmailById` sends **one** message per call. No batching, no retry, no sleep. Batching is done one level up by `NewEmailController.processScheduledEmails`.

### 2.5 Error handling
- Every method `throws Exception`; nothing is caught inside `GmailService`. The only local guard is the explicit `throw new Exception("Draft raw content is missing.")` when `draft.getMessage().getRaw() == null`.
- `NewEmailController.processScheduledEmails` catches per-email exceptions and marks the queue row `FAILED` + `errorMessage = e.getMessage()`; `getSentCountFromGmail` swallows exceptions and returns an **empty map** (200 OK with no data).

### 2.6 Security-relevant behaviour
- OAuth client id/secret/refresh token come from the `gmail_master` table (plaintext columns `clientId`, `secret`, `refreshToken`, `accessToken`).
- `getAllGmail()` returns the raw `GmailMaster` entities → **`clientId`, `secret`, `refreshToken`, `accessToken` are serialized to the HTTP response** of `POST /getAllGmail`. `GmailMaster.toString()` also prints them.
- `sendDraftEmailById` prints the recipient to stdout: `System.out.println("Email sent to " + toEmail);` (PII in logs, no secret).
- `InternetAddress.parse(toEmail, false)` — `strict=false`, so malformed addresses are tolerated rather than rejected.

### 2.7 Bugs / race conditions
1. **No credential caching** — `createGmailService` is called at the top of every public method and performs a fresh `credential.refreshToken()` (one extra HTTPS round-trip to `oauth2.googleapis.com` per API operation). With N queued emails this is N token refreshes.
2. `getAllDraftSubjectsWithIds` → N+1 API calls (one `drafts.get` per draft) and `drafts.list` is **not paginated** (first 100 drafts only).
3. `sendDraftEmailById` only substitutes `{{Domain}}` in `text/html` body parts and in the subject; `text/plain` parts inside a multipart draft are **not** substituted.
4. `mimeMessage.setContent(body, mimeMessage.getContentType())` in the non-multipart branch reuses the original content type verbatim (e.g. `text/plain; charset=us-ascii`).
5. Base64 is `Base64.getUrlDecoder()/getUrlEncoder()` (URL alphabet) while Gmail's `raw` is standard base64url — works, but there is no `mimeMessage.saveChanges()` in the `String` branch, so the rewritten body may not be re-mime-encoded before `writeTo`.
6. `getEmailSendStats` counts *messages* in `in:sent`, which includes messages sent from other devices/tools — the "remaining quota" is an estimate only, and the cap (500/1500) is a hard-coded guess, not the real Gmail limit.
7. `createGmailService(email)` → `gmailMasterRepository.findByEmail(email)` may return `null` → NPE on `gmailMaster.getClientId()`.

---

## 3. `service/EmailExtractionService.java`
Path: `.../service/EmailExtractionService.java` (49 lines)

### 3.1 Purpose and API
`@Service`. Single method:
```java
public CompletableFuture<ConcurrentHashMap<String, String>> extracted(Row row, ConcurrentHashMap<String, String> map)
```
**Dead code**: it is `@Autowired` into `EmailService` (`emailExtractionService`) but never called from anywhere in the app; `EmailService.extracted(...)` is a separate, duplicated implementation.

### 3.2 External systems
JSoup GET `https://` + `row.getCell(0).getStringCellValue()` (no timeout, follows redirects by default). No DB, no HTTP server, no filesystem.

### 3.3 Threading
`CompletableFuture.supplyAsync(...)` → **ForkJoinPool.commonPool()**. No `@Async`, no executor injection, no bounded queue.

### 3.4 Concurrency control
None. One task per row; the caller (nobody) would have to aggregate.

### 3.5 Error handling
`catch (Exception e) { e.printStackTrace(); }` — the exception is swallowed and the (possibly partial) map is returned. The method never fails the future.

### 3.6 Security
Unvalidated outbound URL from an uploaded spreadsheet → SSRF/port-scan vector; no scheme/host allow-list. Email regexes can harvest addresses from internal pages.

### 3.7 Bugs
- `Pattern p = Pattern.compile("(0/91)?[7-9][0-9]{9}")` (phone regex) is compiled and **never used** — dead code; the `91` prefix is also written as `0/91` (character class, not alternation) so it would match a literal `/`.
- The map is `ConcurrentHashMap<String,String>` keyed by email → value domain; callers must merge maps themselves.
- No dedupe: the same email found on many pages is inserted many times (map dedupes by key, but the DB insert in `EmailService.extracted` does not).

---

## 4. `service/NewEmailExtractorService.java`
Path: `.../service/NewEmailExtractorService.java` (193 lines) — **this is the live extractor** (used by `POST /emailExtractor` in `EmailController`).

### 4.1 Purpose and API
`@Service`. Reads an Excel of domains, crawls `https://<domain>`, regex-extracts e-mail addresses, bulk-inserts into `email_extractor_data`, updates `email_extractor` job row.

```java
@Async
public void extract(InputStream file, Long loginId)
private String getDomainValue(Cell cell)
private boolean isValidEmail(String email, String domain)
private boolean isUnique(String domain, String email)
private void saveBatch(List<EmailExtractorData> batch)
```
Injected: `@Qualifier("commonEmailExecutor") ExecutorService executor` (= `Executors.newFixedThreadPool(cores*2)`), `EmailExtractorRepository`, `EmailExtractorDataRepository`.
State: `private final ConcurrentMap<String, Set<String>> emailDedupMap = new ConcurrentHashMap<>();` (instance field, never cleared), `SimpleDateFormat sdfDateTime` (shared, not thread-safe), `private static final int BATCH_SIZE = 100` (**declared but never used** — the code uses the literal `100`).

### 4.2 External systems
- **JSoup** → `Jsoup.connect("https://" + domain).timeout(10000).get()` — **10 s timeout** (the only extractor that sets one).
- E-mail regex: `"[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"` applied to `doc.html()` (the whole page, not per-element).
- **MySQL**: `email_extractor` (job header) and `email_extractor_data` (one row per email).

### 4.3 Threading / async
- `@Async` → Spring Boot's auto-configured `applicationTaskExecutor` (core 8, unbounded queue) for the outer method.
- Inner work: `this.executor.submit(() -> {...})` → `commonEmailExecutor` = `Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2)`, **unbounded `LinkedBlockingQueue`**.
- No `@Transactional`; `saveAll` runs in its own repository transaction.

### 4.4 Batching / rate limits / timeouts (exact numbers)
- DB batch trigger: `if (batchList.size() >= 100) { synchronized (list) { saveBatch(new ArrayList<>(batchList)); batchList.clear(); } }` → **batch size 100**; the remainder is flushed after the loop.
- Per-task wait: `future.get(15L, TimeUnit.SECONDS)` → **15 s per domain**; on `TimeoutException` → `future.cancel(true)` and `System.err.println("Timeout: domain task took too long.")`.
- JSoup timeout **10 000 ms**. No inter-request delay, no global rate limit, no backpressure (all rows submitted up front).

### 4.5 Error handling
- Inner task: `catch (Exception e) { return false; }` → the domain counts as *failed*, nothing is logged.
- `saveBatch`: `saveAll` in a try; on exception it falls back to saving rows one by one, logging `System.err.println("Error saving email data: " + d.getEmail());` (leaks the address to stderr).
- Outer: `catch (Exception e) { e.printStackTrace(); }` → if this fires, the `email_extractor` row is left at `"IN PROGRESS"` (never set to FAILED).
- Final status: `processedCount == 0 → "FAILED"`, else `"SUCCESS"` with `noOfDomain = processedCount`.

### 4.6 Security
- SSRF again: `https://` + spreadsheet cell, no host allow-list (internal hosts, cloud metadata `169.254.169.254`, etc.).
- `getDomainValue` accepts `CellType.STRING` and `CellType.NUMERIC` (numeric cells are stringified as a long, e.g. `1234` → `https://1234`).
- No secret handling in this class.

### 4.7 Bugs / races
1. **`emailDedupMap` is a per-JVM singleton, never cleared.** Re-running the same domain in a later job yields **zero** new emails (the `Set.add` returns `false` → filtered out). Memory grows unbounded across jobs.
2. **Flush race**: the size check and the copy/clear are not atomic (the `synchronized` block is on the `Collections.synchronizedList` *wrapper*, and `ArrayList.add` on the wrapper does not hold the same monitor as the wrapper's `synchronized` methods). Two threads can both copy the same 100+ items → **duplicate rows**; a copy taken while another thread adds can throw `ConcurrentModificationException` inside `new ArrayList<>(batchList)`.
3. `noOfDomain` is set to the number of domains that yielded ≥ 1 e-mail, not the number of domains processed → the metric is misleading.
4. `future.get(15, SECONDS)` is called **after** all tasks were submitted, so the effective per-domain budget is 15 s *from the moment of waiting*, not from submission; with a saturated pool, tasks that never started will all "time out" and be cancelled even though they never ran.
5. `future.cancel(true)` interrupts the worker; JSoup's socket read is not interruptible → the thread stays blocked until the 10 s socket timeout.
6. `CommonPool`/fixed pool tasks write to `batchList` from many threads; `Collections.synchronizedList` makes each `add` atomic but the "copy + clear" sequence is not.
7. `getDomainValue` returns `null` for `CellType.BLANK`/`FORMULA`/`BOOLEAN` → the task returns `false` (counted as a failure) for blank rows.
8. The `email_extractor` row is created *before* the file is parsed, so a corrupt workbook still leaves a job row (status `IN PROGRESS`).
9. `SimpleDateFormat` is shared across executor threads → corrupted timestamp strings.

---

## 5. `service/EmailInboxService.java`
Path: `.../service/EmailInboxService.java` (150 lines)

### 5.1 Purpose and API
`@Service`. Pulls the Inbox of every configured mailbox over IMAP and stores messages in `email_inbox`.

```java
@Scheduled(cron="0 0 11 * * *")
public void fetchEmailInbox()
public List<EmailDto> getEmailInbox(Long emailMasterId, String fromDate, String toDate)
private String getTextFromMessage(Message message) throws MessagingException, IOException
private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException
```

### 5.2 External systems (IMAP)
Hard-coded to Gmail, **not** driven by the `email_master` row's host/protocol/port:
```java
properties.put("mail.imap.host", "imap.gmail.com");
properties.put("mail.imap.port", "993");
properties.put("mail.imap.starttls.enable", "true");
properties.put("mail.imap.ssl.trust", "imap.gmail.com");
Session emailSession = Session.getDefaultInstance(properties);
Store store = emailSession.getStore("imaps");
store.connect("imap.gmail.com", emailMaster.getEmail(), emailMaster.getPassword());
Folder inbox = store.getFolder("Inbox");
inbox.open(2);   // Folder.READ_WRITE
```
`emailMaster.getPassword()` is the **SMTP password** reused as the IMAP credential (Gmail app passwords work for both, but Hostinger/Outlook/brevo rows in `email_master` do not).
Search term:
```java
ReceivedDateTerm olderThan = new ReceivedDateTerm(2, new java.util.Date());  // ComparisonTerm.LE
ReceivedDateTerm newerThan = new ReceivedDateTerm(5, new java.util.Date());  // ComparisonTerm.GE
AndTerm andTerm = new AndTerm(olderThan, newerThan);
Message[] messages = inbox.search(andTerm);
```
DB: `email_inbox` (`emailId`, `from`, `subject`, `content`, `date`); delete/replace: `emailInboxRepository.deleteByEmailIdAndDate(email, today)` then `saveAll(...)`.
Filesystem: none. HTML is cleaned with `Jsoup.parse(html).text()`.

### 5.3 Threading / async
`@Scheduled(cron="0 0 11 * * *")` → **11:00:00 every day** (server default timezone). `@EnableScheduling` is on the application class. The scheduler is the single-threaded `ConcurrentTaskScheduler` bean from `cfg/SchedulerConfig` → this job and `NewEmailController.processScheduledEmails` share **one** thread, so a long IMAP run delays the queue processor.

### 5.4 Batching / limits
- No batching, no limit on the number of messages fetched; all matching messages are loaded into memory and inserted with one `saveAll`.
- No socket timeout set on the IMAP session → a stalled server blocks the scheduler thread indefinitely.
- `System.out.println("messages.length---" + messages.length);` is the only observability.

### 5.5 Error handling
Three sibling catch blocks — `NoSuchProviderException`, `MessagingException`, `Exception` — all doing only `e.printStackTrace()`. Consequences:
- The `try` wraps the **whole loop over all mailboxes**, so the first failing mailbox aborts the remaining ones.
- `inbox.close(false)` / `store.close()` are **not** in a `finally` → IMAP connections leak on any exception (and the folder is left open in READ_WRITE).
- `emailInboxList` is declared **outside** the loop and accumulated across mailboxes.

### 5.6 Security
- IMAP password = SMTP password from the DB, reused across protocols; credentials for 41 mailboxes are stored in plaintext (`email_master.password`).
- Inbox content is stored in the DB unencrypted (PII), and `getEmailInbox` returns it to any authenticated user — `getEmailInbox` resolves the mailbox by `emailMasterId` **without checking `loginId`** → cross-tenant read of any mailbox by id guessing.
- `getEmailInbox` ignores the `fromDate`/`toDate` arguments entirely (no filtering).

### 5.7 Bugs
1. **The search term is effectively empty**: `ReceivedDateTerm.LE(now)` AND `ReceivedDateTerm.GE(now)` matches only messages received *exactly* at `now` (second precision) → in practice **0 messages** are ever fetched. The intent was clearly "today" (e.g. `>= startOfDay`).
2. `emailInboxList` is shared across mailboxes and `saveAll(emailInboxList)` runs **inside** the loop → mailbox #2 re-saves mailbox #1's rows; with `@Where(isActive=1)` and no unique constraint this creates duplicates.
3. `emailInbox.setFrom(message.getFrom()[0].toString())` → `ArrayIndexOutOfBoundsException` if a message has no From header.
4. `message.getReceivedDate()` can be `null` → NPE on `.getTime()`.
5. `inbox.open(2)` = READ_WRITE; should be READ_ONLY (1).
6. `getTextFromMimeMultipart` prefers `text/plain` and `break`s, so a multipart/alternative message loses its HTML part; the recursion returns a `String` cast from an `Object` accumulator — fragile but works.

---

## 6. `scraper/service/AmazonScraperService.java`
Path: `.../scraper/service/AmazonScraperService.java` (67 lines)

### 6.1 Purpose and API
**Not a Spring bean** (no `@Service`/`@Component` annotation, no `@Autowired` targets anywhere) — a plain class that must be instantiated manually. No controller in the decompiled tree calls it; the scraper feature is wired only through the Node side.
```java
private static final String SCRAPER_API = "http://localhost:3000/scrape?url=";
private static final String AMAZON_BASE  = "https://www.amazon.in/s";   // UNUSED
private final ObjectMapper objectMapper = new ObjectMapper();

public List<Product> scrapeByKeyword(String keyword, int page)
```

### 6.2 External systems — exact wire format
Java → Node (**GET**, not POST; the URL is the *only* payload):
```java
String query = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
String amazonUrl = "https://www.amazon.in/s?k=" + query + "&page=" + page;
String fullUrl = SCRAPER_API + URLEncoder.encode(amazonUrl, StandardCharsets.UTF_8);   // double-encoded
HttpURLConnection conn = (HttpURLConnection) new URL(fullUrl).openConnection();
conn.setRequestMethod("GET");
conn.setConnectTimeout(10000);
conn.setReadTimeout(15000);
int status = conn.getResponseCode();
if (status != 200) throw new RuntimeException("Scraper API error: HTTP " + status);
```
Effective request: `GET http://localhost:3000/scrape?url=https%3A%2F%2Fwww.amazon.in%2Fs%3Fk%3D<kw>%26page%3D<n>`
- **No auth headers**, no API key, no `Accept` header set.
- Response is read fully into a `StringBuilder` (line-by-line) and parsed with Jackson: `root.get("products")` must be an array; each element → `objectMapper.treeToValue(item, Product.class)`.

Node side (from the JAR, `BOOT-INF/classes/puppeteer/server.js`):
```js
const port = 3000;
app.use((req,res,next)=>{res.header("Access-Control-Allow-Origin","*"); ...});
app.get("/", (req,res)=>res.send("✅ Amazon Scraper API is running"));
app.get("/scrape", async (req,res)=>{
  const { url } = req.query;
  if (!url || !url.startsWith("https://www.amazon."))
      return res.status(400).json({error:"Invalid or missing `url` parameter"});
  const finalUrl = new URL(url);
  const keyword = finalUrl.searchParams.get("k") || "N/A";
  const page = parseInt(finalUrl.searchParams.get("page") || "1", 10);
  const result = await scrapeAmazonProducts(finalUrl.href);
  res.json({ keyword, page, productCount: result.products.length, products: result.products });
});
```
`BOOT-INF/classes/puppeteer/scrape.js` (the actual scraper):
```js
const browser = await puppeteer.launch({ headless: true });
await page.goto(url, { waitUntil: "domcontentloaded", timeout: 30000 });
document.querySelectorAll("div[data-asin][data-component-type=s-search-result]").forEach((el)=>{
  titleEl      = el.querySelector("h2 a span");
  urlEl        = el.querySelector("h2 a");
  imageEl      = el.querySelector("img");
  soldByEl     = el.querySelector(".a-row .a-size-base-plus");
  fulfilledBy  = el.innerHTML.includes("Fulfilled by Amazon") ? "Amazon" : "Other";
  items.push({ title, url: "https://www.amazon.in"+href, image, soldBy, fulfilledBy, sellerDetails: null });
});
```
`BOOT-INF/classes/puppeteer/package.json`: deps `express ^4.21.2`, `puppeteer ^24.14.0`, `puppeteer-extra ^3.3.6`, `puppeteer-extra-plugin-stealth ^2.11.2`, `cors ^2.8.5` (cors is listed but never `require`d — CORS is hand-rolled).

### 6.3 Threading
None — plain synchronous blocking HTTP. No `@Async`, no pool.

### 6.4 Batching / limits
- One HTTP request per `(keyword, page)` call. No batching, no retry, no rate limit, no delay between Amazon requests (each call spawns a full Chromium instance).
- Timeouts: connect **10 000 ms**, read **15 000 ms** (Java side). Puppeteer navigation timeout: **30 000 ms** (Node side) — a page that takes >15 s to respond trips the Java read timeout first.

### 6.5 Error handling
```java
catch (Exception e) { e.printStackTrace(); throw new RuntimeException("Failed to scrape Amazon: " + e.getMessage(), e); }
```
The HTTP-status `RuntimeException` is therefore wrapped in a second `RuntimeException` (message doubled). Stack traces go to stdout; the caller gets a generic `RuntimeException` → HTTP 500 with no detail.

### 6.6 Security
- The Node server binds **all interfaces** on port 3000 with `Access-Control-Allow-Origin: *` and **no authentication** → anyone who can reach the host can use it as an open Amazon-scraping proxy.
- URL validation is a naive prefix check `url.startsWith("https://www.amazon.")` — bypassable (`https://www.amazon.evil.com/...` passes, and `https://www.amazon.@evil.com` style tricks), and it does not prevent the *server* from being pointed at arbitrary hosts after redirect. The Java side always sends `https://www.amazon.in/...`, but the endpoint is publicly reachable.
- No command injection in Java (no `Runtime.exec` here).
- Scraping `amazon.in` from a datacenter IP will typically hit bot-detection; `puppeteer-extra-plugin-stealth` is a dependency but **is not used** in `scrape.js`.

### 6.7 Bugs
1. `AMAZON_BASE` is dead (the URL is rebuilt inline).
2. **JSON contract mismatch**: Node emits `sellerDetails` (always `null`); Java's `Product` expects `sellerInfo` → `sellerInfo` is **always null** after deserialization. Node never emits `price` → `price` always null. `Product` has no `sellerDetails` field, so it is dropped (`@JsonIgnoreProperties(ignoreUnknown=true)`).
3. `conn.disconnect()` is **not** in a `finally` → on any exception the HTTP connection is left to the GC.
4. The whole response is buffered in a `StringBuilder` — no streaming, no size cap (a large result set can OOM).
5. The Amazon URL is double-encoded (`URLEncoder.encode` of an already-encoded string) — harmless because the Node side URL-decodes once, but the `k` value is encoded twice (`%3D` etc. inside), which *does* work since the Node side parses `finalUrl.searchParams.get("k")` after one decode.
6. `new RuntimeException(...)` propagates a checked-style failure through a non-declared method — callers must catch `RuntimeException`.

---

## 7. `scraper/model/Product.java`
Path: `.../scraper/model/Product.java` (260 lines)

- `@JsonIgnoreProperties(ignoreUnknown = true)` on the class and on the nested class.
- Fields (all `String` except the nested object): `title`, `url`, `image`, `price`, `soldBy`, `fulfilledBy`, `sellerInfo`.
- Nested `public static class SellerInfo`: `sellerName`, `sellerRating`, `totalRatings`, `businessName`, `businessAddress`.
- API: standard getters/setters for all 7 + 5 fields; Lombok-style generated `equals`/`hashCode`/`toString` covering **all** fields (including `sellerInfo`), `protected boolean canEqual(Object)`.
- `toString()` prints every field — if a `Product` is ever logged, the (non-secret) scraped data is dumped; no credential exposure here.
- No `@JsonProperty`/`@JsonCreator` → Jackson binds by exact field name; **no `sellerDetails` → `sellerInfo` mapping exists**, so the Node payload's seller block is dropped. `price` is a `String` (so a numeric price from a different scraper would still bind, but Amazon's price is never scraped).
- Equality is field-wise including `sellerInfo` (so two products with the same title but different `sellerInfo` are unequal) — fine for de-dup by title only if you compare titles explicitly.

---

## 8. `cfg/PuppeteerRunner.java`
Path: `.../cfg/PuppeteerRunner.java` (56 lines)

### 8.1 Purpose and API
Starts/stops the Node scraper as a child OS process.
```java
public class PuppeteerRunner {          // NOT a @Component / @Service
    private Process puppeteerProcess;
    public void startNodeServer()
    public void stopNodeServer()
}
```
**Dead code**: nothing in the decompiled tree instantiates it (grep for `PuppeteerRunner|startNodeServer|stopNodeServer` → only the class itself). It is not a Spring bean, has no `@PostConstruct`/`@PreDestroy`, and is never wired.

### 8.2 External systems (process execution)
```java
File nodeDir = new File("src/main/resources/puppeteer");        // relative to the JVM CWD
ProcessBuilder installBuilder = new ProcessBuilder("npm.cmd", "install");
installBuilder.directory(nodeDir);
installBuilder.redirectErrorStream(true);
Process installProcess = installBuilder.start();
... reader.lines().forEach(System.out::println);
if (!installProcess.waitFor(2L, TimeUnit.MINUTES)) {
    System.err.println("⚠️ npm install timed out");
    installProcess.destroy();
    return;
}
ProcessBuilder serverBuilder = new ProcessBuilder("node", "server.js");
serverBuilder.directory(nodeDir);
serverBuilder.redirectErrorStream(true);
this.puppeteerProcess = serverBuilder.start();
new Thread(() -> { /* drain stdout -> System.out */ }).start();
System.out.println("✅ Puppeteer Node server started.");
```
`stopNodeServer()`:
```java
if (this.puppeteerProcess != null && this.puppeteerProcess.isAlive()) {
    this.puppeteerProcess.destroy();     // SIGTERM, no waitFor()
    System.out.println("🛑 Puppeteer Node server stopped.");
}
```

### 8.3 Threading
- A dedicated raw `Thread` (not a pool) is spawned to drain the child's stdout.
- The caller thread blocks up to **2 minutes** on `installProcess.waitFor(2, TimeUnit.MINUTES)`.
- No executor, no `@Async`, no `@Scheduled`.

### 8.4 Timeouts / concurrency
- `npm install` timeout: **2 minutes** (hard `return` on timeout — the server is never started).
- The child `node server.js` is **not** waited on; the method returns immediately after `start()`. No port-readiness probe — the Java client may call `localhost:3000` before Express is listening.

### 8.5 Error handling
`catch (IOException | InterruptedException e) { e.printStackTrace(); }` only. Notably:
- The **exit code of `npm install` is never checked** → a failed install still proceeds to start `node server.js` (which then crashes with MODULE_NOT_FOUND, printed to stdout by the drain thread).
- `InterruptedException` is caught without restoring the interrupt flag.

### 8.6 Security
- **No command injection** — both command vectors are fixed literals; no user input reaches `ProcessBuilder`.
- **Path/CWD dependence**: `new File("src/main/resources/puppeteer")` is relative to the process working directory. In the packaged JAR the assets live at `BOOT-INF/classes/puppeteer/`, so this path **does not exist** at runtime → `npm install` fails ("no such file or directory") → `server.js` is never started. It only works when run from a source checkout.
- **`npm.cmd`** is the Windows batch wrapper → on Linux/macOS `ProcessBuilder("npm.cmd", ...)` throws `IOException` (No such file or directory). The shipped `app.upload.path=C:\deployment` confirms the original target was Windows.
- `npm install` at every boot: unpinned transitive deps (`^` ranges in `package.json`), network required, and **Puppeteer's postinstall downloads Chromium** (~150 MB) — the process blocks startup for up to 2 minutes.
- No `--ignore-scripts`, no lockfile enforcement at runtime (`package-lock.json` is shipped but `npm install` honours it only loosely), no integrity pinning.
- The child process is **not** registered for shutdown (`destroy()` is never called on exit) → the Node process can outlive the JVM; `stopNodeServer()` is never invoked.
- The drain thread is a non-daemon thread → it can keep the JVM alive.

---

## 9. `cfg/SchedulerConfig.java`
Path: `.../cfg/SchedulerConfig.java` (24 lines)

```java
@Configuration
public class SchedulerConfig {
    @Bean
    public TaskScheduler taskScheduler() {
        return new ConcurrentTaskScheduler();
    }
}
```
- **This class contains no cron expression.** It only supplies the `TaskScheduler` bean.
- `ConcurrentTaskScheduler()` with no-arg constructor → **pool size 1** (Spring's `ConcurrentTaskScheduler` default `corePoolSize = 1`).
- Consequence: **all** `@Scheduled` methods share one thread and run strictly one at a time. The IMAP inbox job (can block for minutes on a stalled IMAP server) will delay the 60-second Gmail queue processor, which in turn queues fixed-rate executions back-to-back.
- Because the bean is named `taskScheduler`, it overrides Boot's `TaskSchedulingAutoConfiguration` (which would also be a single-threaded `ConcurrentTaskScheduler` — behaviourally identical here).
- No `@EnableScheduling` here (it is on `EmailSenderApplication`), no `ThreadPoolTaskScheduler`, no pool-size property, no `setErrorHandler` (a throwing job is only logged by Boot's default `TaskUtils`).

### 9.1 What actually runs on a schedule
| Job | Annotation | Trigger | Where |
|---|---|---|---|
| `EmailInboxService.fetchEmailInbox()` | `@Scheduled(cron="0 0 11 * * *")` | **11:00:00 daily** (server TZ) | `service/EmailInboxService.java:62` |
| `NewEmailController.processScheduledEmails()` | `@Scheduled(fixedRate=60000L)` | **every 60 000 ms** | `controller/NewEmailController.java:131` |
| `EmailService.extract(...)` | `@Async` (not scheduled) | on request | `service/EmailService.java:483` |
| `NewEmailExtractorService.extract(...)` | `@Async` (not scheduled) | on request | `service/NewEmailExtractorService.java:64` |

`processScheduledEmails()` detail (the Gmail send loop):
```java
List<EmailQueue> dueEmails = emailQueueRepository.findByStatusAndScheduledTimeBefore("PENDING", LocalDateTime.now());
Map<String,List<EmailQueue>> groupedBySender = dueEmails.stream().collect(groupingBy(EmailQueue::getFromEmail));
// one Callable per (fromEmail) group, each iterating its emails serially
List futures = this.executor.invokeAll(tasks);        // commonEmailExecutor = fixedThreadPool(cores*2)
for (Future future : futures) allProcessed.addAll((Collection) future.get());
emailQueueRepository.saveAll(allProcessed);
```
Status values: `"PENDING"` → `"SENT"` / `"FAILED"` (with `errorMessage`). `fixedRate=60000` means a new run starts 60 s after the previous one *starts* (not after it ends); since the scheduler has 1 thread, a slow run delays the next one.

---

## 10. `cfg/JpaConfigurationMySql.java`
Path: `.../cfg/JpaConfigurationMySql.java` (93 lines)

### 10.1 Purpose and API
Manual JPA wiring (there is no `spring.jpa.*` config in `application.properties`).
```java
@Configuration
@EnableJpaRepositories(basePackages={"com.pg.*"},
    entityManagerFactoryRef="EntityManagerMySql",
    transactionManagerRef="transactionManagerMySql")
@EnableTransactionManagement
public class JpaConfigurationMySql {
    @Autowired private Environment env;

    @Primary @Bean public DataSource mysqlDataSource()
    @Primary @Bean(value={"EntityManagerMySql"}) public LocalContainerEntityManagerFactoryBean entityManagerFactory() throws NamingException
    @Primary @Bean public JpaVendorAdapter jpaVendorAdapter()
    @Primary @Bean @Autowired public PlatformTransactionManager transactionManagerMySql(EntityManagerFactory emf)
    private Properties jpaProperties()
}
```

### 10.2 External systems (MySQL)
```java
dataSource.setDriverClassName(env.getProperty("app.datasource.mysql.driver-class-name"));  // com.mysql.jdbc.Driver
dataSource.setJdbcUrl(env.getProperty("app.datasource.mysql.url"));                      // jdbc:mysql://localhost:3306/emailsender
dataSource.setUsername(env.getProperty("app.datasource.mysql.username"));                // root
dataSource.setPassword(env.getProperty("app.datasource.mysql.password"));                // [redacted - see SECRETS.txt]
```
Effective URL: `jdbc:mysql://localhost:3306/emailsender`, user `root`, password `[redacted - see SECRETS.txt]`.
HikariCP 5.1.0 with **all defaults left in place**: `maximumPoolSize = 10`, `minimumIdle = 10`, `connectionTimeout = 30 000 ms`, `idleTimeout = 600 000 ms`, `maxLifetime = 1 800 000 ms`, `autoCommit = true`, `leakDetectionThreshold = 0` (off). No `dataSourceProperties` (e.g. no `useSSL=false`, no `allowPublicKeyRetrieval`) → with Connector/J 8.0.31 and MySQL 8 the default `caching_sha2_password` auth requires `allowPublicKeyRetrieval=true` or TLS, otherwise the first connect fails with `Public Key Retrieval is not allowed`.

Hibernate properties:
```java
properties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
properties.put("hibernate.hbm2ddl.auto", "validate");
properties.put("hibernate.show_sql", "true");
```
`hbm2ddl.auto=validate` → **the schema must already exist and match the entities** (that is `emailsend21.sql`); Hibernate will not create or alter tables. `show_sql=true` → every statement is printed to stdout (with parameters), i.e. **recipient e-mail addresses and scraped data land in the logs**.

`entityManagerFactory()`:
```java
factoryBean.setDataSource(this.mysqlDataSource());     // self-invocation of a @Bean method (CGLIB-proxied -> same singleton)
factoryBean.setPackagesToScan(new String[]{"com.pg.*"});
```
`transactionManagerMySql` → `JpaTransactionManager` bound to the EMF.

### 10.3 Threading
No executors. `@EnableTransactionManagement` (default `PROXY` mode, proxy-based `@Transactional`); **no service class in the app declares `@Transactional`** → each repository call is its own transaction; multi-step operations (e.g. `emailSend`'s job+status writes) are **not** atomic.

### 10.4 Error handling
No `try/catch` anywhere in this class. `entityManagerFactory()` declares `throws NamingException`. Bean-creation failures surface as context startup failure.

### 10.5 Security
- DB root credentials in `application.properties` inside the JAR (plaintext).
- `hibernate.show_sql=true` → data in logs.
- `hbm2ddl.auto=validate` (safe direction — no auto-mutation).
- `@Primary` on all four beans although there is a single datasource (redundant, but means it is impossible to add a second datasource without untangling it).

### 10.6 Bugs
1. `basePackages={"com.pg.*"}` and `packagesToScan={"com.pg.*"}` are **not valid package names** (wildcards are not supported by Spring's scanners). It happens to work because the scanner converts the base to a resource path `com/pg/*` and expands it, but it is fragile and will silently pick up nothing if the entity packages move. Correct values: `com.pg.seo.entity` / `com.pg.seo.repository`.
2. `com.mysql.jdbc.Driver` is the pre-8.0 driver class name (deprecated shim in Connector/J 8.0.31; **removed in 9.x**). Use `com.mysql.cj.jdbc.Driver`.
3. `factoryBean.setDataSource(this.mysqlDataSource())` calls the `@Bean` method directly (self-invocation). It works only because of `@Configuration` CGLIB proxying; if the class were `@Component`/`@Lite` mode, a second DataSource would be created.
4. `@Autowired` on the `transactionManagerMySql(EntityManagerFactory emf)` **parameter** — parameter injection is resolved by type; there is exactly one EMF, so it binds correctly, but the annotation is redundant.
5. Hikari pool is never explicitly shut down (no `close()` bean) — normally handled by Spring's `DataSource` lifecycle.
6. `hibernate.dialect` set explicitly while `spring.jpa.hibernate.dialect` is not — harmless, but it hard-codes MySQL and defeats Boot's dialect auto-detection.

---

## 11. `cfg/WebMvcConfigruation.java`
Path: `.../cfg/WebMvcConfigruation.java` (56 lines) — note the typo in the class name ("Configruation") is in the original.

```java
@Configuration
@ComponentScan(basePackages={"com.pg.*"})
public class WebMvcConfigruation implements WebMvcConfigurer {
    @Bean public BCryptPasswordEncoder passwordencoder()
    public Executor taskExecutor()                       // NOT a @Bean -> never registered
    @Bean public RestTemplate restTemplate()
    @Bean(name={"commonEmailExecutor"}) public ExecutorService commonEmailExecutor()
}
```

### 11.1 Purpose and API
Cross-cutting MVC/async/executor configuration.

### 11.2 External systems
- `RestTemplate` bean: plain `new RestTemplate()` — **no connect/read timeouts**, no error handler (4xx/5xx throw `HttpClientErrorException`), no interceptors. Used by `EmailSendController.sendExcelData` (see below) and anywhere else that autowires it.
- `BCryptPasswordEncoder` bean: default strength **10** (Spring's default), used by `LoginDetailsServiceImpl`.
- No MySQL / SMTP / filesystem here.

### 11.3 Threading / executors
```java
public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(1);
    executor.setQueueCapacity(502);
    executor.setThreadNamePrefix("GithubLookup-");
    executor.initialize();
    return executor;
}
```
**Missing `@Bean` → this pool does not exist.** The name/prefix are a copy-paste from the Spring reference docs ("GithubLookup"). Because it is not a bean, `@Async` falls back to Spring Boot's auto-configured `applicationTaskExecutor` (alias `taskExecutor`, core 8, max `Integer.MAX_VALUE`, queue `Integer.MAX_VALUE`, prefix `task-`). **If you add the missing `@Bean` while rebuilding, you would change @Async behaviour from 8 threads to 1 thread + 502 queue** — do not add it blindly.

```java
@Bean(name={"commonEmailExecutor"})
public ExecutorService commonEmailExecutor() {
    int coreCount = Runtime.getRuntime().availableProcessors();
    return Executors.newFixedThreadPool(coreCount * 2);
}
```
→ `newFixedThreadPool(cores * 2)` threads with an **unbounded** `LinkedBlockingQueue`. Used by `NewEmailExtractorService` (domain scraping) and `NewEmailController.processScheduledEmails` (Gmail sending).
**No `@PreDestroy`/`shutdown()`** → the pool's threads are non-daemon and are never shut down on context close (JVM hangs on `context.close()` unless the JVM is killed, and on a redeploy the threads leak).

`@ComponentScan(basePackages={"com.pg.*"})` on a class that is itself inside the `@SpringBootApplication` scan → redundant (Boot already scans `com.pg.seo` and below).

### 11.4 Error handling
None. `RestTemplate` has no `setErrorHandler`, no `ClientHttpRequestFactory` timeouts, no `RetryTemplate`.

### 11.5 Security
- `BCryptPasswordEncoder` default cost 10 (the stored hashes in `login` are cost 12 — a mismatch is fine for verification, but new passwords are weaker than the existing ones).
- `SecurityConfiguration` also declares `public PasswordEncoder passwordencoder()` **without `@Bean`** → only the `WebMvcConfigruation` one is registered. No conflict, but the duplicate is confusing; if someone adds `@Bean` to the `SecurityConfiguration` one, there will be **two beans of type `PasswordEncoder`** (one typed `BCryptPasswordEncoder`) → injection ambiguity.
- `SecurityConfiguration` (same package) permits `/`, `/resources/**`, `/webjars/**`, `/assets/**`, `/static/assets/**`, `/api/v1/getBarcode4j`, `/word`; everything else requires authentication; `formLogin` at `/login`, `defaultSuccessUrl("/welcome")`, `maximumSessions(1)`, `SessionCreationPolicy.IF_REQUIRED`. No CSRF config visible (default CSRF is on for non-GET).
- `LoginDetailsServiceImpl` enforces the **licence lock**: MAC address must equal `mac` (`[redacted - see SECRETS.txt]`) and the `license` JWT is only **decoded** (not signature-verified) with an `exp` check → the licence is forgeable, and the app is hard-bound to one NIC.

### 11.6 Bugs
1. `taskExecutor()` missing `@Bean` (dead code, misleading name).
2. `commonEmailExecutor()` is a raw `ExecutorService` bean with no lifecycle management and an unbounded queue.
3. `RestTemplate` without timeouts is a hang risk on any outbound call (the `EmailSendController` call to Google Apps Script is the live consumer).
4. Class name typo `WebMvcConfigruation`.
5. `implements WebMvcConfigurer` with **no overridden methods** → CORS, resource handlers, interceptors, argument resolvers and async support config are all absent.

---

## 12. Direct answers to the four specific questions

### Q1 — How does the email send loop work? (sync / batched / per-recipient errors)

There are **two entirely separate send paths** in the app.

**Path A — SMTP campaign (`EmailService.emailSend`, entry point `POST /sendEmail`)**
1. Guard: sum of `email_send_job.no_of_email` for the **current month** for that `loginId`; if `> 50000` → return the string `"Daily Limit Reached "` and send nothing. (Monthly cap, mislabelled "Daily".)
2. Load `EmailMaster` by `fromEmail`; build a **fresh** `JavaMailSenderImpl` (host/port/username/password/protocol/auth/starttls all from the `email_master` row).
3. Load the template by id; branch on `emailType`:
   - `"G"` (group) → `clientMasterRepository.findByGroupCodeAndLoginId(group, loginId)` (native SQL, `isactive=1`).
   - otherwise (individual) → `Arrays.asList(request.getClient())`, each element parsed as a **client master id**.
4. Insert one `email_send_job` row: `status="In Progress"`, `type="Group"|"Individual"`, `noOfEmail=<count>`, `date=today`.
5. **The loop is fully synchronous and strictly serial, on the HTTP request thread.** For each recipient:
   - build the Thymeleaf context `{name, webLink, content}` and render `mailtemplate`;
   - `helper.setTo(...)`, `helper.setText(html, true)`, `helper.setSubject(templateName)`, `helper.setFrom(fromEmail)` on the **shared** `MimeMessage`/`MimeMessageHelper`;
   - `mailSender.send(message)` — one new SMTP connection per recipient, no keep-alive, no sleep, no rate limit;
   - **per-recipient error handling**: `emailSender` wraps everything in `try { … } catch (Exception e) { e.printStackTrace(); return "FAILURE"; }`. A failure never aborts the campaign; the recipient is recorded in `email_send_status` with `status="FAILURE"` (success = `"SUCCESS"`). The exception detail is only in the stack trace on stdout — **not** in the DB and **not** returned to the client.
   - after each send: `findById(jobId).get()` → set `noOfSuccess` → `save(job)` (3 DB round-trips per recipient) and insert the `email_send_status` row.
6. After the loop: reload the job, set `status="Success"`, save, return `"SUCCESS"`.
- **No retry, no batching, no backoff, no concurrency** — N recipients = N sequential SMTP handshakes on one request thread. A 50 000-recipient group holds the HTTP request open for hours and will be killed by any proxy timeout.
- Failure modes that escape the loop entirely (job left `"In Progress"`, HTTP 500): null `emailMaster`/`emailTemplate`/`clientMaster`, null `emailType`/`client[]`, `Long.parseLong` on a non-numeric client id.

**Path B — Gmail draft sending (`NewEmailController.processScheduledEmails` + `GmailService.sendDraftEmailById`)**
1. `POST /send` parses an Excel (col 0 = domain, col 1 = e-mail) and inserts one `email_queue` row per recipient with `status="PENDING"` and `scheduledTime` (parsed from the `date` request param, ISO-8601 local date-time; `LocalDateTime.now()` if absent).
2. A **scheduler** job runs every **60 000 ms**: select `PENDING` rows with `scheduledTime < now`, **group by `fromEmail`**, and submit one `Callable` per sender group to `commonEmailExecutor` (fixed pool, `cores*2`). Inside a group, emails are sent **one at a time, serially**; different sender groups run **in parallel**.
3. Per email: `gmailService.sendDraftEmailById(fromEmail, toEmail, draftId, domain)` → creates a Gmail client (which **refreshes the OAuth token on every call**), `GET /gmail/v1/users/me/drafts/{id}?format=raw`, base64url-decodes the raw RFC822, parses it into a `MimeMessage`, replaces `{{Domain}}` in the subject and in every `text/html` body part, sets the `To` recipient, re-encodes to base64url and `POST /gmail/v1/users/me/messages/send`.
4. Per-email error handling: `catch (Exception e) { email.setStatus("FAILED"); email.setErrorMessage(e.getMessage()); }` — the failure is **persisted** (unlike Path A) and the loop continues. On success → `status="SENT"`. All updated rows are written once at the end with `saveAll`.
5. **No retry**: a failed send is `FAILED` forever; there is no dead-letter/requeue. **No quota enforcement**: `getEmailSendStats` (500 personal / 1500 workspace per 24 h) is only *displayed* on `/last-24-hours/live`; the sender does not consult it. **No delay between sends** in a group → a large group can trip Gmail's 500/day limit.

### Q2 — How is the Puppeteer scraper invoked? (script path, port, payload, parsing)

- **Java never spawns Node in the live code path.** `cfg/PuppeteerRunner` is the only place that would (`npm.cmd install` + `node server.js` in `src/main/resources/puppeteer`), and it is **not a Spring bean and is never instantiated** — it is dead code in the shipped build.
- The Java side talks to an **already-running Node/Express server over plain HTTP**:
  - **Port 3000, path `/scrape`, method GET** (not POST), no body, no auth headers.
  - Exact URL built by `AmazonScraperService.scrapeByKeyword(keyword, page)`:
    `http://localhost:3000/scrape?url=` + `URLEncoder.encode("https://www.amazon.in/s?k=" + URLEncoder.encode(keyword,"UTF-8") + "&page=" + page, "UTF-8")`
  - Timeouts: connect **10 000 ms**, read **15 000 ms**; non-200 → `RuntimeException("Scraper API error: HTTP " + status)`.
- **Node script path in the artifact**: `BOOT-INF/classes/puppeteer/server.js` (Express, `const port = 3000`) + `BOOT-INF/classes/puppeteer/scrape.js` (the Puppeteer logic) + `package.json` (`"main": "server.js"`, `"scripts": {"start": "node server.js"}`).
- **Node → Amazon**: `GET /scrape?url=…` validates `url.startsWith("https://www.amazon.")`, then `scrape.js` does `puppeteer.launch({headless:true})`, `page.goto(url,{waitUntil:"domcontentloaded",timeout:30000})`, and extracts from `div[data-asin][data-component-type=s-search-result]`: title (`h2 a span`), url (`"https://www.amazon.in" + h2 a[href]`), image (`img[src]`), soldBy (`.a-row .a-size-base-plus`), fulfilledBy (`"Amazon"` if the element's HTML contains `"Fulfilled by Amazon"` else `"Other"`), `sellerDetails: null`.
- **Node → Java JSON**: `{"keyword":…,"page":…,"productCount":N,"products":[{title,url,image,soldBy,fulfilledBy,sellerDetails}]}`.
- **Parsing back into Java**: Jackson `ObjectMapper.readTree(body)` → `root.get("products")` (must be an array) → `objectMapper.treeToValue(item, Product.class)` for each element → `List<Product>`.
- **Contract gaps to fix in the rebuild**: Node sends `sellerDetails`, Java expects `sellerInfo` (⇒ always `null`); Node never sends `price` (⇒ always `null`); Node never sends the seller fields (`sellerName`, `sellerRating`, `totalRatings`, `businessName`, `businessAddress`) that `SellerInfo` models.
- The Node server has **no authentication** and `Access-Control-Allow-Origin: *`; it is reachable by anything that can route to port 3000.

### Q3 — How are Google OAuth tokens refreshed, and where are they persisted?

- **Persisted in MySQL**, table `gmail_master` (entity `com.pg.seo.entity.GmailMaster`, `@Where(clause="isActive =1")`), columns: `gmail_master_id`, `email`, `client_id`, `secret`, `refresh_token`, `access_token`, `is_active`, `created_by/date`, `modified_by/date`. **All plaintext.** Seeded rows (per `SECRETS.txt`): `[redacted - see SECRETS.txt]` and `[redacted - see SECRETS.txt]`, each with its own client id, client secret, refresh token and a (stale) access token.
- **Refresh logic** (`GmailService.createGmailService(String email)`, duplicated verbatim in `cfg/GmailServiceFactory`):
  ```java
  GmailMaster gmailMaster = gmailMasterRepository.findByEmail(email);
  GoogleCredential credential = new GoogleCredential.Builder()
      .setClientSecrets(gmailMaster.getClientId(), gmailMaster.getSecret())
      .setTransport(GoogleNetHttpTransport.newTrustedTransport())
      .setJsonFactory(GsonFactory.getDefaultInstance())
      .build()
      .setRefreshToken(gmailMaster.getRefreshToken());
  credential.refreshToken();                       // POST https://oauth2.googleapis.com/token
  return new Gmail.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, credential)
      .setApplicationName("MAIL ELEXYT").build();
  ```
  Uses the **legacy `GoogleCredential` + `google-api-client` refresh-token flow** (not `UserCredentials`/`ServiceAccountCredentials`).
- **Key behaviours to reproduce exactly**:
  1. The refresh token is the **only** persisted token used at runtime. The `access_token` column is **never written by the application** — the freshly minted access token lives only in the in-memory `GoogleCredential`.
  2. `credential.refreshToken()` is called **on every `createGmailService()`**, and `createGmailService()` is called at the start of **every** public method of `GmailService` ⇒ **one token round-trip per Gmail API operation** (draft listing = 1 + N extra refreshes). There is no credential cache and no proactive refresh.
  3. The refresh token is the long-lived secret; revoking the OAuth client in Google Cloud Console invalidates it.
  4. `newTrustedTransport()` accepts the platform trust store (no custom SSL).
  5. No scope is requested on the refresh (the scopes are bound to the refresh token), so the app inherits whatever the original grant had (Gmail read/send/modify).
- **Security issue to fix**: `GmailService.getAllGmail()` → `POST /getAllGmail` returns the raw entities, so `clientId`, `secret`, `refreshToken`, `accessToken` are sent to the browser. Same for `EmailMaster.password` via `/getEmailMaster` and `/getEmailMasterById`. DTOs or `@JsonIgnore` are required.

### Q4 — What is the SchedulerConfig cron schedule, and what jobs run on it?

- `cfg/SchedulerConfig` **has no cron at all**. It only declares the scheduler bean:
  ```java
  @Bean public TaskScheduler taskScheduler() { return new ConcurrentTaskScheduler(); }   // pool size 1
  ```
- The **only cron expression in the whole application** is in `EmailInboxService`:
  ```java
  @Scheduled(cron="0 0 11 * * *")   // 11:00:00 every day, server default timezone
  public void fetchEmailInbox()
  ```
  It IMAPs `imap.gmail.com:993` (`imaps`, folder `Inbox`, `open(2)` = READ_WRITE) for **every** row in `email_master` using that row's e-mail + password, searches with `ReceivedDateTerm(LE, now) AND ReceivedDateTerm(GE, now)` (effectively "received exactly now" ⇒ normally **zero** messages), and replaces `email_inbox` rows for that mailbox via `deleteByEmailIdAndDate(email, today)` + `saveAll`.
- The other scheduled job is **fixed-rate, not cron**, and lives in the controller:
  ```java
  @Scheduled(fixedRate=60000L)     // every 60 seconds
  public void processScheduledEmails()
  ```
  It picks up `email_queue` rows with `status='PENDING'` and `scheduledTime < now`, groups them by `fromEmail`, and sends each group through `GmailService.sendDraftEmailById` on the `commonEmailExecutor` pool, then `saveAll` with `SENT`/`FAILED` + `errorMessage`.
- Because the scheduler is **single-threaded**, the 11:00 IMAP job and the 60-second Gmail job contend for the same thread; a hung IMAP connection (no socket timeout) blocks the Gmail sender indefinitely.

---

## 13. Cross-cutting risk list for the rebuild (ranked)

| # | Risk | Where | Impact |
|---|---|---|---|
| 1 | `gmail_master` / `email_master` secrets returned to the browser (`clientId`, `secret`, `refreshToken`, `accessToken`, SMTP password) | `GmailService.getAllGmail`, `EmailService.getEmailMasters` | Full account takeover of 2 Google accounts + 41 mailboxes |
| 2 | Plaintext DB credentials, OAuth tokens, SMTP app passwords, licence JWT, WordPress token in the packaged `application.properties` / `emailsend21.sql` | artifact | Full compromise; all of it must be rotated (see `docs/SECRET-HARVEST.md`) |
| 3 | Cross-tenant data access: `removeClient`/`removeClientGroup` ignore `loginId`; `getEmailInbox` ignores `loginId` | `EmailService` | Tenant A can delete/see tenant B's data |
| 4 | SSRF via spreadsheet-driven `Jsoup.connect("https://" + cell)` | `EmailService.extracted`, `NewEmailExtractorService.extract` | Internal network probing from the app host |
| 5 | `emailDedupMap` is a per-JVM singleton that is never cleared | `NewEmailExtractorService` | Silent loss of results on re-runs; unbounded memory growth |
| 6 | IMAP search term matches nothing; `emailInboxList` shared across mailboxes | `EmailInboxService` | Inbox feature is effectively broken / duplicates rows |
| 7 | SMTP send loop is serial on the request thread with no rate limit | `EmailService.emailSend` | Request timeouts, provider bans, 50 000-recipient campaigns cannot complete |
| 8 | `email_send_job.no_of_success` can be `null` → NPE in monthly report | `EmailService.getMonthlyEmailReport` | Dashboard 500 |
| 9 | `@Async` + shared `SimpleDateFormat` | `EmailService`, `NewEmailExtractorService` | Corrupted timestamps under load |
| 10 | `commonEmailExecutor` never shut down; `PuppeteerRunner` leaks the Node child process | `WebMvcConfigruation`, `PuppeteerRunner` | JVM does not exit cleanly; orphan processes |
| 11 | `hibernate.show_sql=true` | `JpaConfigurationMySql` | Recipient addresses / scraped leads in the logs |
| 12 | Licence enforced by MAC address + an **unverified** HS256 JWT | `LoginDetailsServiceImpl` | Trivially forgeable licence; app refuses to run on a different NIC |
| 13 | `RestTemplate` without timeouts; SMTP/IMAP without timeouts | `WebMvcConfigruation`, `EmailService`, `EmailInboxService` | Thread hangs indefinitely |
| 14 | Puppeteer flow is Windows-only (`npm.cmd`, `src/main/resources/puppeteer` CWD path) and unwired | `PuppeteerRunner` | Scraper does not start in a packaged JAR on Linux/macOS |

**Not verified / out of scope of this pass**: the exact `id`/`name` of the `mailtemplate` Thymeleaf resolver (the JAR ships `templates/mailtemplate.html` and Boot's default `templates/` prefix resolves it); whether `EmailService.extract` (`@Async`) is reachable from any HTTP endpoint (grep shows no caller — it is dead code, superseded by `NewEmailExtractorService`); the `emailsend21.sql` DDL (not read, only the `SECRETS.txt` summary of it).

Skipped: `login/*` classes beyond the licence/MAC check, `templates/*.html` beyond `mailtemplate.html`, and the `SECRETS.txt` credential values themselves (I read the config keys, not the live tokens). The one thing I could not fully pin down is whether the *shipped* `email_send_job.no_of_success` column is `NOT NULL` — that decides whether the monthly-report NPE is theoretical or immediate.
</task_result>
</task>