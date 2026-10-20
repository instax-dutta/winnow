# Security

The threat model and the controls of `winnow`. Every boundary below has a control, and
every control says what breaks without it.

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
  `BCryptPasswordEncoder`, at strength 12. One bean, so the encoder that hashes and the
  encoder that verifies are the same one, and the cost is explicit rather than the
  framework default.
- **Form login.** `GET /login` renders the form, `POST /login` processes it. The username
  parameter is `username` and the password parameter is `password`.
- **One concurrent session per user.** `maximumSessions(1)` with
  `SessionCreationPolicy.IF_REQUIRED`. The limit only holds if the expired sessions are
  evicted, so both a `SessionRegistry` and an `HttpSessionEventPublisher` are declared in
  `security/SecurityConfig`. Without them the registry grows without bound and users are
  spuriously logged out as the count is reached.
- **Session fixation protection at the Spring default.** The session id changes on login.
- **Account lockout.** `security/AuthenticationEventRecorder` listens to the framework's
  authentication events, so the counter moves for every way into the application and not
  only for the login form. The username comes from the authentication token the framework
  built, not from a request parameter, so a caller cannot move the counter of an account
  they did not try to log into. Once the count reaches
  `seo.security.max-failed-logins` (default 5) the account is locked for
  `seo.security.lock-duration` (default 15 minutes).
- **One generic failure message.** Every rejected login gets the same message and the same
  redirect: an unknown user, a wrong password, a disabled account and a failed lockout are
  indistinguishable to the caller. A caller who can tell an unknown user from a wrong
  password can also tell which accounts exist.

## Authorization

**The four roles**, seeded by `V1__baseline.sql` and named by `security/RoleNames`:

| Code | What it may do |
|---|---|
| `ROLE_ADMIN` | Everything, including account administration and the user list. |
| `ROLE_MANAGER` | Approve or reject a template, run a campaign, create and edit clients, groups, templates and accounts. |
| `ROLE_OPERATOR` | Create and edit clients, groups, templates and accounts. May not approve a template. |
| `ROLE_VIEWER` | Read everything the tenant owns. May not change anything. |

The four roles are the whole authorization vocabulary, and every rule in the application is
expressed in it. A role that nothing checks is data, not a control: the role codes are a
constant in `RoleNames`, they are seeded by the migration, and every fine-grained rule below
is a `@PreAuthorize` over that vocabulary. The coarse path rules cannot stand on their own,
because "is the user logged in?" is not a decision about what the user may do.

**The filter chain** (`security/SecurityConfig`) carries the coarse path rules only:

| Rule | Paths |
|---|---|
| `permitAll` | `/login`, `/assets/**`, `/favicon.ico`, `/error` |
| `authenticated` | `/dashboard`, `/clients/**`, `/groups/**`, `/templates/**`, `/accounts/**`, `/campaigns/**`, `/outbox/**`, `/extraction/**`, `/inbox/**`, `/api/**`, and everything else |
| Form login | `/login`, with a success handler and a failure handler |

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
the owner id is passed down explicitly and the repository filters on it. That is the reason
the tenant id is an argument to every service method rather than something read out of a
session.

## CSRF

**CSRF is enabled.** It is not configured anywhere, so it stays at the Spring default, and
the token travels two ways:

- In a form: the `_csrf` hidden field that Thymeleaf adds to a form that declares
  `th:action`.
- In a header: `X-CSRF-TOKEN`, which the front end reads from the
  `<meta name="_csrf" th:content="${_csrf.token}">` tag every page declares in its head.

Every mutating endpoint needs one. `src/test/java/io/github/instaxdutta/winnow/integration/CsrfProtectionIT.java`
asserts that every one of them rejects a request without a token.

**A missing token is answered with a redirect to the login page.** That is Spring Security's
own behaviour and it is deliberate here: because `invalidSessionUrl` is configured, a missing
token is read as a lost session, the session is dropped and the caller is sent to the login
form. The mutating endpoint is never executed either way. A request with a **wrong** token,
or a request from a caller who holds no role, is answered with 403.

Every mutating endpoint is a POST, a PUT or a DELETE, so none of them is reachable by a
plain link or by an image tag on another site. Every page declares the token and the front
end sends it, so a legitimate JSON or multipart POST is not rejected.

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

The crawl URL is never built from a spreadsheet cell without going through this check, so a
cell can never reach the loopback interface or the metadata address.

## The upload and download controls

- **In-memory parsing.** An uploaded workbook is parsed from its input stream. It is never
  written to disk, and no file name is ever used to build a path.
- **The size is capped** twice. The Spring multipart limit is 20 MB, and
  `web/UploadGuard` refuses anything over 10 MB before the service is called.
- **The type is checked** at the edge: the content type must be one of the accepted
  spreadsheet types and the extension must be `.xlsx` or `.xls`.
- **No path is ever built from a user-supplied value.** No export endpoint builds a file
  name from a request parameter, so no request can traverse out of the upload directory.
- **A download is scoped to the tenant.** Every export is built from the tenant's own rows,
  so an exported workbook holds that tenant's addresses and nobody else's.

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
an inbound mail never reaches the database and never reaches the browser. Every branch of the
message reader is covered, so no delivery shape is a way around it.

**The template body is operator-authored HTML.** It is rendered raw, because a template is
supposed to be HTML. The controls around it are that only an operator or a manager can
write a template, that only a manager or an administrator can approve one, that a campaign
refuses a template that is not `APPROVED`, that the body is escaped when the recipient's
fields are substituted into it, and that the substituted values themselves are HTML-escaped
by `mail/MailTemplateRenderer`, which has no raw mode.

**The outbound body is escaped at substitution time.** `mail/MailTemplateRenderer.render`
replaces every `{{Name}}` placeholder and HTML-escapes the value that goes in.

**There is no open redirect.** Every redirect target in the application is a fixed path:
`/dashboard`, `/login`, `/login?error`, `/login?logout`. No redirect target is derived from
a request parameter.

**There is no SQL injection.** Every query is a derived query or a parameterised JPQL query.
There is no native SQL in the application at all.

## The open source decision

**There is no licence, no activation and no machine fingerprinting.** Authentication is a
password and a role, and nothing else gates the application. A gate on the machine rather
than on the person protects nothing and blocks everything: it fails on a virtual machine
with a different virtual MAC, on a machine with a different NIC order, and on every machine
once a token expires. A signature that is never verified is not a control either.

So the application has no `NetworkInterface` call and no `getHardwareAddress` call, and
`tools/verify/code-hygiene.mjs` fails the build if one reappears, or if a licence check
reappears.

**Authentication and role-based authorization are not licensing, and they stay.** The role
vocabulary is the whole authorization model: the role codes are a constant in `RoleNames`,
they are seeded by the migration, and every fine-grained rule is a `@PreAuthorize` over
that vocabulary. A role that nothing checks is data, not a control.

`seo.bootstrap.*` exists only to create the first administrator on a fresh install. It is a
deployment convenience. It is not an activation mechanism, and the application starts
normally without it.

## The known limitations that remain

- **The file-based secret store is not a KMS.** `config/FileSecretStore` is a directory of
  files with POSIX permissions. The value is protected by the filesystem's access control
  and by the fact that the directory is not in the repository, and by nothing else. There is
  no envelope encryption, no key version and no audit trail of who read a secret. A store
  backed by a key management service would close that, and it is the obvious thing to
  contribute: `config/SecretStore` is the interface, `config/SecretStoreConfig` selects the
  implementation from `seo.secret-store.type`, and nothing in the service layer changes when
  a new one arrives.
- **A cluster needs one shared store, and the start-up check says so.** The file store is
  local state, so two instances on two hosts do not share it unless they mount the same
  volume. `config/SecretStoreHealth` checks the store at start-up and fails the boot with a
  message that names the fix, rather than letting the first affected mail fail hours later
  with no obvious cause. `SEO_SECRET_STORE_INSTANCES` tells the application how many
  instances sit behind the store, and it warns at start-up when that is more than one.
  Point `SEO_SECRET_STORE_PATH` at a shared volume, or contribute a store that does not
  need one.
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
- **The template body is sanitised on write, and the formatting it needs survives.**
  `service/HtmlSanitiser` cleans the body when it is created and when it is edited, so a
  script tag, an event handler, a `javascript:` link or an iframe cannot reach the preview
  pane. The safelist keeps the formatting an email body actually needs, including tables
  and images, so cleaning does not quietly break the template. The outbound mail carries
  the same cleaned body.
- **`UploadGuard` trusts the declared content type.** It checks the extension and the
  content type that the client declared. A file whose declared type is a spreadsheet but
  whose content is not will be rejected by the parser, but the parser is what has to notice
  it, not the guard. There is no magic-byte check.
- **The application trusts its own database.** Anyone with write access to the database can
  change a tenant's `owner_user_id`. The application is the isolation boundary, not the
  database.
- **The Amazon sidecar is a separate service with its own attack surface.** It drives a
  real browser, which is more dangerous than the application itself. It binds to loopback by
  default, sends no CORS header, takes a search term rather than a URL so a caller can never
  choose the address the server visits, and bounds the keyword, the page number, the product
  count, the navigation time and the total request time. The port it is published on is
  loopback only. What remains is that a browser renders whatever Amazon serves it, and the
  sidecar is a convenient thing to point at a hostile page if it is ever exposed.
- **The scraped address list is personal data.** `extraction_result` holds contact
  addresses harvested from third-party web pages. There is no retention policy and no
  suppression list, and the operator is the one who decides whether to crawl a domain at
  all.
