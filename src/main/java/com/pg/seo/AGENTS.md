# src/main/java/com/pg/seo/

## Purpose

The Java implementation of `seo-0.1.0`. Every package here has one job, and the package
boundary is the contract - a class does not reach across it for convenience.

## Ownership

| Package | Owns | Never does |
|---|---|---|
| `domain/` | JPA entities, enums, audited base types | HTTP, transactions, service calls |
| `repository/` | Data access for one aggregate | Business rules, tenant resolution |
| `service/` | Business rules, transaction boundaries, tenant enforcement | Returning entities directly to the web layer |
| `web/` | HTTP contract, request/response mapping, principal resolution | Business rules, direct repository access |
| `security/` | Authentication, authorization, principal model, CSRF/session policy | Business rules |
| `mail/` | Provider adapters (SMTP, Gmail), template rendering, quota accounting | Tenant rules, scheduling |
| `extraction/` | Domain crawling, email extraction, scraper client, URL allow-list | Direct database writes outside a service |
| `scheduler/` | Scheduled job triggers and their idempotency | Business rules |
| `config/` | Beans, properties binding, infrastructure | Anything domain-specific |

## Local Contracts

### Layering

```
web  ->  service  ->  repository  ->  domain
         |
         +-> mail | extraction (through service, never directly)
```

- `web` may call `service` and read the authenticated principal. Nothing else.
- `service` may call `repository`, `mail`, `extraction`, and other services.
- `repository` may call `repository` and `domain` only.
- A dependency that points the other way is a bug. There are no exceptions.

### Tenant model

- The tenant is the authenticated user. `security/AuthPrincipal` carries `userId`, `username`
  and the granted authorities.
- Every tenant-owned entity has an `owner_user_id` column and every query that returns one
  filters on it. `repository` methods take the owner id as their first argument.
- Global entities (nothing in 0.1.0 today) must be explicitly listed in
  `docs/ARCHITECTURE.md` as global, with the reason.

### Entities

- Timestamps are `Instant` mapped to `DATETIME(6)`. Never `String`, never `java.sql.Date`.
- Soft delete is `deleted_at DATETIME(6) NULL` plus an explicit `deletedAt IS NULL` predicate
  in the repository method. No Hibernate `@Where` - it is silently bypassed by native queries.
- Statuses are Java enums mapped to `VARCHAR` (not MySQL `ENUM`, so a new value is a code
  change rather than a migration).
- Secrets are never entity fields. An entity that needs a credential holds a `secretRef` string.

### Error handling

- Services throw `seo` domain exceptions: `NotFoundException`, `ValidationException`,
  `ForbiddenException`, `ConflictException`.
- `web/GlobalExceptionHandler` maps them to HTTP status plus the `ApiResponse` envelope.
- A service catches an exception only when it can genuinely recover, and it logs at WARN with
  the context needed to act. It never returns a partially-built entity.

## Work Guidance

- Adding a service method: tenant id in, tenant-filtered query, explicit transaction
  annotation when more than one write happens, and a test that asserts the tenant filter.
- Adding an entity: it needs `owner_user_id` (unless documented global), `created_at`,
  `updated_at`, `created_by`, `updated_by`, and a matching Flyway migration.
- Before adding a field to an entity, check whether it is a secret. If it is, it belongs in
  the secret store and the entity holds a reference.

## Verification

- `./mvnw verify` from the repository root.
- Each package has unit tests under `src/test/java/com/pg/seo/<package>/`.

## Child DOX Index

| Path | Scope |
|---|---|
| `src/main/java/com/pg/seo/domain/` | Entity and enum rules, audited base type, soft-delete contract |
| `src/main/java/com/pg/seo/model/` | DTO contract, what a DTO may and may not carry |
| `src/main/java/com/pg/seo/repository/` | Repository method naming, tenant filter contract, query rules |
| `src/main/java/com/pg/seo/service/` | Service responsibilities, transaction boundaries, tenant enforcement |
| `src/main/java/com/pg/seo/web/` | Endpoint conventions, response envelope, error mapping |
| `src/main/java/com/pg/seo/security/` | Authentication, authorization rules, principal model |
| `src/main/java/com/pg/seo/mail/` | Provider adapters, credential handling, quota accounting |
| `src/main/java/com/pg/seo/extraction/` | Crawler, allow-list, extraction rules |
| `src/main/java/com/pg/seo/scheduler/` | Scheduled job contracts and idempotency |
| `src/main/java/com/pg/seo/config/` | Bean and property conventions |
