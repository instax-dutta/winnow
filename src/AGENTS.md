# src/

## Purpose

Application source for `winnow`. Holds the Java implementation, the runtime resources
(templates, static assets, Flyway migrations, configuration) and the test suite.

## Ownership

| Path | Owner | Scope |
|---|---|---|
| `main/java/io/github/instaxdutta/winnow/` | application code | domain model, services, web layer, security, mail, extraction |
| `main/resources/` | runtime resources | Thymeleaf templates, CSS/JS, Flyway migrations, `application*.yml`, Puppeteer scraper |
| `test/java/io/github/instaxdutta/winnow/` | test suite | unit and integration tests, hermetic (H2) |

## Local Contracts

### Java packages under `main/java/io/github/instaxdutta/winnow/`

| Package | Contains | Rule |
|---|---|---|
| `domain/` | JPA entities, enums, value objects | Entities are tenant-owned unless explicitly documented as global. No `String` timestamps. No `@Where` class filter - soft delete is an explicit predicate. |
| `repository/` | Spring Data JPA repositories | Every method that touches tenant-owned data takes the tenant id. No `select *` native SQL. No native SQL at all unless a JPQL query cannot express it. |
| `service/` | Business logic | The only place transactions are declared. Tenant filter is applied here, not only in the controller. |
| `web/` | `@RestController` and `@Controller` | Controllers hold no business logic and no direct repository access. They resolve the tenant from the authenticated principal, never from a request parameter. |
| `security/` | Spring Security configuration, principal, authorization | This is the only place that decides who may call what. |
| `mail/` | SMTP sender, Gmail sender, template rendering | Holds provider credentials in memory only. Never logs a credential. |
| `extraction/` | Domain-to-email extraction and the Amazon scraper client | All outbound URLs pass through the allow-list in `extraction/UrlAllowList`. |
| `scheduler/` | Scheduled jobs | Reads work from a table; never holds work in memory across a restart. |
| `config/` | Bean definitions, properties, infrastructure wiring | No business logic. |

### Resource rules (`main/resources/`)

- `templates/` - Thymeleaf. One template per screen. No inline `style` or `script` blocks larger
  than a few lines; shared JS lives in `static/assets/js/`.
- `static/assets/css/` - one stylesheet, `app.css`. No framework CDN dependency at runtime.
- `static/assets/js/` - ES modules, one per screen area, loaded with `type="module"`.
- `db/migration/` - Flyway migrations. `V<n>__<description>.sql`. Never edit an applied
  migration; add a new one.
- `application.yml` - defaults only. Anything environment-specific or secret is read from the
  environment or the secret store, never hardcoded.

## Work Guidance

- New endpoint checklist: tenant filter, no state change on GET, no secret in the response body,
  a `@JsonIgnore` on every secret-bearing field of any entity that can be serialized, and a test.
- When adding a column, add a Flyway migration in the same change.
- When renaming a column, update the entity, the migration and the native query together.
- Prefer `java.time` (`Instant`, `LocalDateTime`, `OffsetDateTime`) over `Date`/`Timestamp`.
- Log at INFO for lifecycle events, WARN for recoverable failures, ERROR for data loss. Never log
  a recipient list, a token or a credential.

## Verification

- `./mvnw verify` runs the whole suite: unit tests, the H2-backed integration tests, and the
  Flyway migration check.
- `./mvnw test -Dtest=ClassName` runs one test class while iterating.
- Every test must run without network access and without the remote MySQL container.

## Child DOX Index

| Path | Scope |
|---|---|
| `src/main/java/io/github/instaxdutta/winnow/` | Java packages, layering rules, entity and service contracts |
| `src/main/resources/` | Templates, static assets, migrations, configuration |
| `src/test/` | Test suite conventions and fixtures |
