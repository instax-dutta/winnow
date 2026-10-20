# src/main/resources/

## Purpose

Everything the application serves or reads at runtime: templates, static assets, database
migrations and configuration.

## Ownership

| Path | Owns |
|---|---|
| `templates/` | Thymeleaf server-rendered pages |
| `static/assets/css/` | Stylesheets |
| `static/assets/js/` | Client-side ES modules |
| `static/assets/img/` | Images and icons |
| `db/migration/` | Flyway migrations |
| `application.yml` | Default configuration |
| `puppeteer/` | The optional Amazon scraper sidecar (Node) |

## Local Contracts

### Templates
- One template per screen, named after the screen. A fragment is `fragments/<name>.html`.
- A page declares the CSRF token in its head:
  `<meta name="_csrf" th:content="${_csrf.token}">`.
- Every value that came from a user is escaped by default. `th:utext` is used only for a
  field whose contract is "trusted HTML", and that field is sanitised on write.
- No inline `style` or `script` beyond a few lines. Shared behaviour lives in
  `static/assets/js/`.

### Static assets
- No CDN dependency at runtime. Everything is served from this directory.
- One stylesheet, `app.css`. No CSS framework.
- JavaScript is ES modules loaded with `type="module"`, one file per screen area.

### Migrations
- `V<n>__<snake_case_description>.sql`. Never edit an applied migration.
- Every migration is tested against a clean MySQL 8 schema and against H2 in MySQL mode.
- Destructive changes are split: add, backfill, then drop.
- Every table is `utf8mb4_0900_ai_ci`. Every text column gets an explicit length.
- Foreign keys get an index. Every tenant-owned table gets an index on `owner_user_id`.
- Every tenant-owned table gets `created_at`, `updated_at`, `created_by`, `updated_by` and
  `deleted_at`.

### Configuration
- `application.yml` holds defaults only. Secrets come from the environment.
- Profiles: `application-dev.yml`, `application-test.yml`, `application-prod.yml`.

## Work Guidance

- Changing a template that a test asserts against means updating the test in the same change.
- New migrations run in the test suite, so a broken migration fails the build immediately.

## Verification

- `./mvnw verify` - the integration tests run Flyway against a clean schema, so a migration
  that is invalid or that only works on a dirty database fails the build.

## Child DOX Index

This directory has no child DOX files.
