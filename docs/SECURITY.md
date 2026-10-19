# Security

The threat model and the controls of `seo-0.1.0`. What the original did and what changed is
recorded here, because the original's holes are the reason most of these controls exist.

The full record of the original's behaviour is in
`docs/reverse-engineering/02-controllers-and-security.md`. The credential exposure is in
`docs/SECRET-HARVEST.md`.

## The trust boundaries

| Boundary | What crosses it | What the control is |
|---|---|---|
| **Browser to server** | HTML form posts, JSON bodies, multipart uploads, the session cookie | Form login, CSRF tokens, the role checks, the upload guard, and the rule that no `@GetMapping` changes state. |
| **Server to database** | SQL over JDBC | The schema, the foreign keys, the unique constraints, and the tenant filter in every repository method. The database password comes from the environment and is never in a file. |
| **Server to the secret store** | A reference in, a value out | `config/SecretStore` and `config/FileSecretStore`. One file per secret, mode 0600 in a 0700 directory, named by the SHA-256 of the reference. The value is read from disk on every call and never cached. |
| **Server to the SMTP provider** | The credential, the recipient, the body | The adapter resolves the credential at call time, holds it for the duration of the call and drops it. Explicit connect, read and write timeouts. |
| **Server to the Gmail provider** | The OAuth client secret, the refresh token | The same. The access token is minted in memory by the refresh and is never persisted. |
| **Server to the scraped internet** | A GET for a domain page | `extraction/UrlAllowList`: the scheme, the host suffix list and the unconditional private-range block, with every resolved address checked, every redirect re-validated, an explicit timeout and a response size cap. |
| **Server to the scraper sidecar** | A GET with a keyword in a query parameter | Loopback only, with a request timeout. The keyword never reaches the host or the path. |
| **Operator to the database** | Direct SQL access | Out of scope. The schema holds no credential, so a database dump holds no credential. |
| **Inbound mail to the database** | A message body from an unknown sender | `Jsoup.clean` with a basic safelist at capture time, before the body is stored. |

## The credential model

**Nothing is stored in the database and nothing is stored in a committed file.**

1. **The entity holds a reference, not a value.** `smtp_account.secret_ref`,
   `gmail_account.secret_ref` and `gmail_account.refresh_token_ref` are strings. There is no
   password column, no client secret column and no token column anywhere in
   `V1__baseline.sql`.
2. **The secret store holds the value.** `config/FileSecretStore` writes one file per secret
   into `SECRET_STORE_PATH`. The file name is the SHA-256 hex of the reference, so a
   reference can never escape the store directory and the directory listing discloses
   nothing about what is stored. The file is mode 0600 inside a 0700 directory, and a write
   goes to a temporary file that is then renamed, so a reader never sees a half written
   secret. The value is read from disk on every `resolve` and is never cached, so a value
   rotated on disk takes effect on the next call.
3. **The adapter resolves it at call time and drops it.**
   `mail/SmtpMailSender` and `mail/GmailMailSender` take the account repository, the
   `SecretStore` and the mail properties, and they resolve the credential inside the send.
   It is never held on a field and never cached.
4. **No DTO ever carries a credential.** The `model/` package is explicit about this: an
   account DTO carries a boolean `hasCredential` and nothing else. There is no password
   field, no client secret field and no refresh token field in any request or response
   shape. A credential in a request record is write-only; the service stores it and drops
   it.
5. **No log line carries a credential.** Neither a token, a password hash, a session id, a
   recipient list nor the verbatim response of a mail server is ever logged. A failure
   message carries the type of the failure.
6. **The secret store never logs a value**, and the account service logs the reference, not
   the value.

## Authentication

- **BCrypt at cost 12.** `security/PasswordConfig` declares exactly one
   `BCryptPasswordEncoder`, at strength 12. The original shipped two encoder beans, one of
   which was missing its `@Bean` annotation and so never existed, and the live one used the
   default cost of 10.
- **Form login.** `GET /login` renders the form, `POST /login` processes it. The username
  parameter is `username` and the password parameter is `password`.
- **One concurrent session per user.** `maximumSessions(1)` with
  `SessionCreationPolicy.IF_REQUIRED`. The original configured the same limit but declared
  neither a `SessionRegistry` nor an `HttpSessionEventPublisher`, so expired sessions were
  never evicted from the registry, it grew without bound, and users were spuriously logged
  out as the count was reached
  (`reference/decompiled/com/pg/seo/cfg/SecurityConfiguration.java:66`). Both beans are
  declared in `security/SecurityConfig` now.
- **Session fixation protection at the Spring default.** The session id changes on login.
  The original also had this, by accident of never configuring it.
- **Account lockout.** `security/AuthenticationEventRecorder` listens to the framework's
  authentication events, so the counter moves for every way into the application and not
  only for the login form. The username comes from the authentication token the framework
  built, not from a request parameter, so a caller cannot move the counter of an account
  they did not try to log into. Once the count reaches
  `seo.security.max-failed-logins` (default 5) the account is locked for
  `seo.security.lock-duration` (default 15 minutes). The original had no lockout at all.
- **One generic failure message.** Every rejected login gets the same message and the same
  redirect: an unknown user, a wrong password, a disabled account and a failed lockout are
  indistinguishable to the caller. The original was also generic, but for the wrong reason:
  its licence and MAC failures were swallowed into the same message, so an operator whose
  licence had expired had no diagnostic at all.

## Authorization

**The four roles**, seeded by `V1__baseline.sql` and named by `security/RoleNames`:

| Code | What it may do |
|---|---|
| `ROLE_ADMIN` | Everything, including account administration and the user list. |
| `ROLE_MANAGER` | Approve or reject a template, run a campaign, create and edit clients, groups, templates and accounts. |
| `ROLE_OPERATOR` | Create and edit clients, groups, templates and accounts. May not approve a template. |
| `ROLE_VIEWER` | Read everything the tenant owns. May not change anything. |

The original had `ROLE_ADMIN`, `ROLE_EMP`, `ROLE_MANG` and `ROLE_HR`, and exactly one
endpoint had a role requirement: `GET /` required `hasRole("USER")`, which no seeded role
could satisfy, so every user got a 403 on the home page
(`reference/decompiled/com/pg/seo/cfg/SecurityConfiguration.java:62`). The other three roles
were never referenced by any matcher or annotation. The whole authorization model was "is
the user logged in?", plus a tenant filter that was applied inconsistently.

**The filter chain** (`security/SecurityConfig`) carries the coarse path rules only:

| Rule | Paths |
|---|---|
| `permitAll` | `/login`, `/assets/**`, `/favicon.ico`, `/error` |
| `authenticated` | `/dashboard`, `/clients/**`, `/groups/**`, `/templates/**`, `/accounts/**`, `/campaigns/**`, `/outbox/**`, `/extraction/**`, `/inbox/**`, `/api/**`, and everything else |
| Form login | `/login`, with a success handler and a failure handler |

The original additionally permitted `/api/v1/getBarcode4j` and `/word`, two URLs with no
controller and no route behind them.

**The fine rules** live on the controllers and the services as `@PreAuthorize`, expressed
with the vocabulary in `RoleNames` so both layers speak one language:

| Operation | Expression |
|---|---|
| Reading | `isAuthenticated()` |
| Creating or editing templates, clients, groups and accounts | `hasAnyRole('ROLE_OPERATOR','ROLE_MANAGER','ROLE_ADMIN')` |
| Approving or rejecting a template | `hasAnyRole('ROLE_MANAGER','ROLE_ADMIN')` |
| Administering accounts | `hasRole('ROLE_ADMIN')` |
| Listing the users | `hasRole('ROLE_ADMIN')` |

**The tenant model is the actual isolation mechanism.** The role checks decide whether a
caller may perform a kind of operation. The tenant filter decides whose data the operation
touches. A caller with `ROLE_ADMIN` still only ever sees their own tenant's rows, because
the owner id is passed down explicitly and the repository filters on it. That is what the
original got wrong in eleven places, and it is the reason the tenant id is an argument to
every service method rather than something read out of a session.

## CSRF

**CSRF is enabled.** It is not configured anywhere, so it stays at the Spring default, and
the token travels two ways:

- In a form: the `_csrf` hidden field that Thymeleaf adds to a form that declares
  `th:action`.
- In a header: `X-CSRF-TOKEN`, which the front end reads from the
  `<meta name="_csrf" th:content="${_csrf.token}">` tag every page declares in its head.

Every mutating endpoint needs one. `src/test/java/com/pg/seo/integration/CsrfProtectionIT.java`
asserts that every one of them rejects a request without a token.

**A missing token is answered with a redirect to the login page.** That is Spring Security's
own behaviour and it is deliberate here: because `invalidSessionUrl` is configured, a missing
token is read as a lost session, the session is dropped and the caller is sent to the login
form. The mutating endpoint is never executed either way. A request with a **wrong** token,
or a request from a caller who holds no role, is answered with 403.

The original also left CSRF at the default, but its six state-changing GET endpoints were
exempt from it and could be triggered from any other site with an image tag, and every
legitimate JSON or multipart POST returned 403 unless the client sent a token the shipped
front end was never confirmed to send.

## The SSRF control

`extraction/UrlAllowList.check(URL)` runs before every outbound fetch. Three rules, in
order:

1. **The scheme must be `https` or `http`.** Anything else is refused.
2. **The host must be on the allow-list.** When
   `seo.scraper.allowed-host-suffixes` is empty, `seo.scraper.allow-arbitrary-hosts` decides
   whether any public host may be fetched. When the list is not empty, only a host that
   equals a listed suffix or ends in `.` plus a listed suffix may be fetched.
3. **Every address the host resolves to must be a public address.** The name is resolved and
   every answer is checked, so a public name that points at `127.0.0.1` is refused. A host
   that does not resolve is treated as local and refused.

**The private-range block is unconditional and cannot be switched off.** Rejected:
`0.0.0.0/8`, `10.0.0.0/8`, `127.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`,
`169.254.0.0/16` (which includes the cloud metadata address), multicast `224.0.0.0/4`,
`::1`, `::`, `fc00::/7`, `fe80::/10`, multicast IPv6, and the IPv4-mapped form of any of
those.

**Redirects are re-validated.** A redirect is followed at most
`seo.extraction.max-redirects` times (default 2), and each hop goes through the same check.
A redirect into a private range is refused.

**The response size is capped** at `seo.extraction.max-response-bytes` (default 2 MiB) and
every fetch has an explicit connect timeout (5 s) and read timeout (10 s).

The original built the crawl URL straight from a spreadsheet cell and fetched it with no
check at all, so a cell could reach the loopback interface or the metadata address
(`reference/decompiled/com/pg/seo/service/NewEmailExtractorService.java:64`). The dead
variant of the same code set no timeout at all.

## The upload and download controls

- **In-memory parsing.** An uploaded workbook is parsed from its input stream. It is never
  written to disk, and no file name is ever used to build a path.
- **The size is capped** twice. The Spring multipart limit is 20 MB, and
  `web/UploadGuard` refuses anything over 10 MB before the service is called.
- **The type is checked** at the edge: the content type must be one of the accepted
  spreadsheet types and the extension must be `.xlsx` or `.xls`.
- **No path is ever built from a user-supplied value.** The original wrote its export to
  `app.upload.path + filename`, where the filename embedded the raw `fromDate` and
  `toDate` request parameters, so `fromDate=2024-01-01/../../evil` was a path traversal
  primitive, and it put that filename unquoted into a `Content-Disposition` header
  (`reference/decompiled/com/pg/seo/service/EmailService.java:553`). The rebuild has no
  export endpoint that builds a filename from a request parameter.
- **The original's download endpoint returned every tenant's data.** `POST /downloadFile`
  read the `loginId` out of the session and then never used it, so the exported workbook
  held every tenant's scraped addresses.

## The XSS policy

**`th:text` everywhere by default.** Every value that came from a user is escaped by
Thymeleaf unless the field's contract is trusted HTML.

**`th:utext` is used in exactly three places**, and for two fields:

| Template | Field | Why |
|---|---|---|
| `templates.html` | `contentHtml` | The preview of the template body. |
| `template-approvals.html` | `contentHtml` | The preview of a template awaiting approval. |
| `inbox.html` | `bodyHtml` | The body of a captured inbound message. |

**The inbound message body is sanitised on write.** `scheduler/InboxPollJob` runs
`Jsoup.clean(text, Safelist.basic())` over the body before it is stored, so a script tag in
an inbound mail never reaches the database and never reaches the browser. The original
sanitised the multipart branch of its message reader with `Jsoup.parse(html).text()` but not
the single-part `text/html` branch, so a stored XSS from any inbound mail was possible
(`reference/decompiled/com/pg/seo/service/EmailInboxService.java:112`).

**The template body is operator-authored HTML.** It is rendered raw, because a template is
supposed to be HTML. The controls around it are that only an operator or a manager can
write a template, that only a manager or an administrator can approve one, that a campaign
refuses a template that is not `APPROVED`, that the body is escaped when the recipient's
fields are substituted into it, and that the substituted values themselves are HTML-escaped
by `mail/MailTemplateRenderer`, which has no raw mode. The original rendered the body with
`th:utext` and no escaping at all, so a compromised template injected arbitrary HTML into
every recipient's mail.

**The outbound body is escaped at substitution time.** `mail/MailTemplateRenderer.render`
replaces every `{{Name}}` placeholder and HTML-escapes the value that goes in. The original
injected the domain value from the uploaded spreadsheet straight into the Gmail draft body
with a plain `String.replace`, which was an HTML injection into the sent message
(`reference/decompiled/com/pg/seo/service/GmailService.java:110`).

**There is no open redirect.** Every redirect target in the application is a fixed path:
`/dashboard`, `/login`, `/login?error`, `/login?logout`. No redirect target is derived from
a request parameter. The original was the same on this point.

**There is no SQL injection.** Every query is a derived query or a parameterised JPQL query.
There is no native SQL in the rebuild at all.

## The open source decision

**There is no licence, no activation and no machine fingerprinting.** This is a security
improvement, not a missing feature.

The original refused to authenticate unless the host's MAC address matched a value in its
configuration, and unless an HS256 licence token had not expired. Three things were wrong
with that at once:

- **The signature was never verified.** The original called `JWT.decode()` from the auth0
  library, which decodes and does not verify. Anyone who could edit the configuration file
  could unlock the application with a self-made token
  (`reference/decompiled/com/pg/seo/login/LoginDetailsServiceImpl.java:73`).
- **It was a licensing control dressed up as security.** It protected nothing. It broke the
  application on any machine that was not the licensed one, including a virtual machine
  with a different virtual MAC, and on a machine with a different NIC order.
- **It locked the application out entirely.** The shipped licence expired on 2025-09-18, so
  the reference build could not authenticate anyone, on any machine.

The rebuild has no `NetworkInterface` call and no `getHardwareAddress` call.
`tools/verify/code-hygiene.mjs` fails the build if one reappears, or if a licence check
reappears.

**Authentication and role-based authorization are not licensing, and they stay.** The
original's genuine weakness was that the role vocabulary was disconnected from the
authorization rules: the roles were data that nothing checked. That is fixed by making the
role codes a constant in `RoleNames`, by seeding them, and by expressing every fine-grained
rule as a `@PreAuthorize` over that vocabulary.

`seo.bootstrap.*` exists only to create the first administrator on a fresh install. It is a
deployment convenience. It is not an activation mechanism, and the application starts
normally without it.

## The findings from the original that were fixed

| Original behaviour | Evidence | The fix |
|---|---|---|
| `/getEmailMasterById` returned another tenant's SMTP account, with the plaintext password. | `reference/decompiled/com/pg/seo/controller/EmailController.java:324` | No endpoint returns a credential. The account DTO has a `hasCredential` flag. The lookup is tenant-scoped. |
| `/getAllGmail` returned every Google OAuth client id, client secret, refresh token and access token. | `reference/decompiled/com/pg/seo/controller/NewEmailController.java:83` | The same. |
| `POST /downloadFile` exported every tenant's scraped addresses. | `reference/decompiled/com/pg/seo/controller/EmailController.java:402` | The results of one run, scoped to the tenant. |
| Six endpoints mutated state over GET and were exempt from CSRF. | `reference/decompiled/com/pg/seo/controller/EmailController.java:162,225,241,257,330,352` | Every mutation is a POST, PUT or DELETE. |
| The MAC lock and the unverifiable licence token. | `reference/decompiled/com/pg/seo/login/LoginDetailsServiceImpl.java:65,73` | Both removed. `tools/verify/code-hygiene.mjs` enforces it. |
| SMTP passwords and OAuth tokens stored in plaintext columns. | `reference/decompiled/com/pg/seo/service/EmailService.java:131` | A `secret_ref` into the secret store. |
| `hibernate.show_sql = "true"` printed every insert, including the SMTP passwords, to stdout. | `reference/decompiled/com/pg/seo/cfg/JpaConfigurationMySql.java:80` | `seo.jpa.show-sql` is `false`. |
| The MySQL root password, the licence, the MAC and a WordPress API token in the shipped `application.properties`. | `docs/SECRET-HARVEST.md` | Not carried over. All of it must be rotated. |
| `Jsoup.connect("https://" + cell)` fetched a spreadsheet cell. | `reference/decompiled/com/pg/seo/service/NewEmailExtractorService.java:64` | `extraction/UrlAllowList`, plus timeouts, a size cap and redirect re-validation. |
| `/sendData` posted the uploaded file to `https://script.google.com/macros/s/YOUR_SCRIPT_ID/exec` with no timeout and no allow-list. | `reference/decompiled/com/pg/seo/controller/EmailSendController.java:52` | The endpoint is not carried over. |
| The scraper sidecar bound every interface with a wildcard CORS header and no authentication. | `reference/decompiled/com/pg/seo/service/AmazonScraperService.java:25` | The sidecar is reached over loopback with a timeout. |
| The export filename was built from raw request parameters and put unquoted into a response header. | `reference/decompiled/com/pg/seo/service/EmailService.java:553` | No path is built from user input. |
| No `SessionRegistry` and no `HttpSessionEventPublisher` behind `maximumSessions(1)`. | `reference/decompiled/com/pg/seo/cfg/SecurityConfiguration.java:66` | Both beans are declared. |
| No account lockout. | `reference/decompiled/com/pg/seo/login/LoginDetailsServiceImpl.java` | `AuthenticationEventRecorder` plus `seo.security.max-failed-logins`. |
| The stored inbound mail body was rendered with `th:utext` unsanitised in the single-part branch. | `reference/decompiled/com/pg/seo/service/EmailInboxService.java:112` | `Jsoup.clean` with a basic safelist at capture time. |
| The Gmail draft body had a spreadsheet value substituted with a plain `replace`. | `reference/decompiled/com/pg/seo/service/GmailService.java:110` | `MailTemplateRenderer` HTML-escapes every substituted value. |
| No CSRF token on the JSON and multipart POSTs, so every one of them returned 403. | `reference/decompiled/com/pg/seo/cfg/SecurityConfiguration.java:62` | Every page declares the token and the front end sends `X-CSRF-TOKEN`. |
| The scoper of the send loop was a request thread, with no SMTP timeout and no per-account pacing. | `reference/decompiled/com/pg/seo/service/EmailService.java:82` | The dispatcher claims work with a conditional update, the timeouts are explicit, and the quota is per account and per window. |

The full list, with the rest of the defects that are not security defects, is in
`docs/MIGRATION-FROM-0.0.1.md`.

## The known limitations that remain

- **The file-based secret store is not a KMS.** `config/FileSecretStore` is a directory of
  files with POSIX permissions. The value is protected by the filesystem's access control
  and by the fact that the directory is not in the repository, and by nothing else. There is
  no envelope encryption, no key version and no audit trail of who read a secret.
- **`FileSecretStore` is fine for a single host and wrong for a cluster.** It is local
  state. Two instances of the application on two hosts do not share a secret store, and a
  secret written on one is invisible on the other. A clustered deployment needs a shared
  store, and the `config/SecretStore` interface is the seam for one. Until that exists, run
  one instance, or mount the same directory on every instance with the consequence that a
  rotation on one host has to be repeated on the others.
- **The inbound message body is stored as HTML.** `inbox_message.body_html` is the sanitised
  HTML of the message, not plain text. It is rendered with `th:utext` after the sanitisation,
  so it is not an injection vector, but it is still stored and still served to the browser
  with a script-capable content type. A deployment that must not store HTML at all would
  need the plain-text branch to be preferred.
- **The outgoing mail can carry operator-authored HTML by design.** A template body is
  HTML and it is sent as HTML. The controls are the role vocabulary, the approval workflow,
  the refusal to send a template that is not approved, and the escaping of the substituted
  recipient values. An administrator who approves a hostile template can still send
  hostile HTML to a recipient. That is the feature, not a defect.
- **The template body is not sanitised on write.** The resource contract in
  `src/main/resources/AGENTS.md` requires a field rendered with `th:utext` to be sanitised
  on write. The inbound message body is. The template body is not yet, because a template is
  operator-authored HTML that the operator means to send as HTML. Until it is, a template
  body is raw HTML in the preview pane. Anyone who can edit a template can put a script tag
  in the preview of the approvals page, which is a same-origin script execution against the
  reviewing administrator. That is the one gap in this document, and it is a gap in the
  code, not only in the description.
- **`UploadGuard` trusts the declared content type.** It checks the extension and the
  content type that the client declared. A file whose declared type is a spreadsheet but
  whose content is not will be rejected by the parser, but the parser is what has to notice
  it, not the guard. There is no magic-byte check.
- **The application trusts its own database.** Anyone with write access to the database can
  change a tenant's `owner_user_id`. The application is the isolation boundary, not the
  database.
- **The scraped address list is personal data.** `extraction_result` holds contact
  addresses harvested from third-party web pages. There is no retention policy and no
  suppression list, and the operator is the one who decides whether to crawl a domain at
  all.
