# Plan: seo-0.1.0 rebuild

Scope: seo-v01-rebuild
Depth: tree 2
Mode: orchestrated

## Contract

**Interfaces**

- `pom.xml` - Spring Boot 3.5.x parent, Java 21. Dependencies: web, thymeleaf, data-jpa,
  security, validation, mail, flyway-core + flyway-mysql, mysql-connector-j, jsoup, poi-ooxml,
  google-api-services-gmail, google-api-client, lombok, and test scope: spring-boot-starter-test,
  spring-security-test, h2.
- Layering: `web -> service -> repository -> domain`, with `mail` and `extraction` reachable
  only from `service`.
- Response envelope: `ApiResponse<T> { status, message, data }` on every JSON endpoint.
- Tenant: `AuthPrincipal { userId, username, authorities }` resolved from the Spring Security
  principal and passed explicitly into every service method.
- Persistence: Flyway owns the schema. `V1__baseline.sql` creates the whole 0.1.0 schema.
  Hibernate `ddl-auto` is `validate` in dev/prod and `none` in test.
- Provider abstraction: `MailSender { SendResult send(SendRequest) }` with `SmtpMailSender`
  and `GmailMailSender` implementations.
- Extraction: `UrlAllowList` gates every outbound URL; `DomainCrawler` returns matches only.

**Ownership** (repository-relative, disjoint)

| Leaf | Owns |
|---|---|
| 1.1.1 | `pom.xml`, `src/main/java/com/pg/seo/domain/**`, `src/main/java/com/pg/seo/repository/**`, `src/main/resources/db/migration/**` |
| 1.1.2 | `src/main/java/com/pg/seo/config/**`, `src/main/java/com/pg/seo/security/**`, `src/main/resources/application*.yml` |
| 1.1.3 | `src/main/java/com/pg/seo/service/**`, `src/main/java/com/pg/seo/mail/**` |
| 1.1.4 | `src/main/java/com/pg/seo/web/**`, `src/main/java/com/pg/seo/extraction/**`, `src/main/java/com/pg/seo/scheduler/**` |
| 1.1.5 | `src/main/resources/templates/**`, `src/main/resources/static/**` |
| 1.2.1 | `src/test/**`, `tools/verify/**` |

**Dependencies** - 1.1.2, 1.1.3, 1.1.4, 1.1.5 each need the domain and repository types from
1.1.1. 1.2.1 needs everything.

**Host launch mode** - Codex native subagents, sequential fallback.

**Wave policy** - wave 1: 1.1.1 (foundation, judged by the orchestrator because the whole
data model hangs off it). wave 2: 1.1.2, 1.1.3, 1.1.4, 1.1.5 in parallel, each with a disjoint
ownership set. wave 3: 1.2.1 (tests) after wave 2. Maximum host concurrency 4.

**Toolchain** - Java 21, Maven wrapper only (`./mvnw`), Node 20+ for the verify scripts, MySQL 8
on the build host at `127.0.0.1:3307` in the dedicated `seo-v01-mysql` container. Build
working directory is `~/seo-rebuild/app` on `tejes@pelican`. Nothing else on that host may be
started, stopped or reconfigured.

**Conventions** - `java.time` only, no `SimpleDateFormat`. No `printStackTrace`, no
`System.out`. Lombok for entities and DTOs. Package-private where possible. Responses are
DTOs, never entities.

**Manual review** - the orchestrator reviews every leaf's diff against the DOX contracts in
`src/main/java/com/pg/seo/AGENTS.md` and its package AGENTS.md before the leaf is verified.

## Current contract inventory

Contract revision: 1.

| ID | Required outcome or constraint | Owner | Observing gate | Disposition | Revision |
|---|---|---|---|---|---|
| C1 | The original artifact is decompiled and its behaviour is recorded | 1.1.1 | `docs/reverse-engineering/` exists with three analyses | ACTIVE | 1 |
| C2 | Every hardcoded token, key and password is inventoried in one inspectable file | 1.2.1 | G10 | ACTIVE | 1 |
| C3 | The application keeps the original product surface | 1.1.4 | G12 | ACTIVE | 1 |
| C4 | No credential exists in source | all | G4 | ACTIVE | 1 |
| C5 | Tenant isolation holds on every read and write | 1.1.3 | G7 | ACTIVE | 1 |
| C6 | State changes are never reachable by GET | 1.1.4 | G5 | ACTIVE | 1 |
| C7 | The schema is owned by Flyway and applies to a clean MySQL 8 | 1.1.1 | G8 | ACTIVE | 1 |
| C8 | The application starts and serves its login page | 1.1.4 | G3 | ACTIVE | 1 |
| C9 | Every mutating endpoint enforces CSRF | 1.1.2 | G9 | ACTIVE | 1 |
| C10 | The DOX tree is complete and consistent | orchestrator | G6 | ACTIVE | 1 |
| C11 | The hard rules in the root AGENTS.md are machine enforced | 1.2.1 | G5 | ACTIVE | 1 |
| C12 | The documentation set is complete and current | orchestrator | G11 | ACTIVE | 1 |
| C13 | A clean checkout builds without a local Maven cache | 1.2.1 | G13 | ACTIVE | 1 |
| C14 | No outbound request is made to a user-supplied URL without an allow-list | 1.1.4 | G5 | ACTIVE | 1 |
| C15 | The build runs on the remote host without disturbing anything else on it | orchestrator | G1, G13 | ACTIVE | 1 |

## State vocabulary

Leaf state is exactly one of WAITING, READY, IN-FLIGHT, VERIFIED, ABANDONED.
Branch state is exactly one of OPEN, VERIFIED, ABANDONED.

## Tree

- 1 seo-0.1.0 rebuild .............. GATES.md
  - 1.1 implementation ............. gates/node-1.1.md
    - 1.1.1 data foundation ........ gates/leaf-1.1.1.md
    - 1.1.2 config and security .... gates/leaf-1.1.2.md
    - 1.1.3 services and mail ...... gates/leaf-1.1.3.md
    - 1.1.4 web, extraction, jobs ... gates/leaf-1.1.4.md
    - 1.1.5 templates and assets .... gates/leaf-1.1.5.md
  - 1.2 verification ............... gates/node-1.2.md
    - 1.2.1 tests and verify tools . gates/leaf-1.2.1.md

## Leaf dispatch table

| Leaf | Owns | Needs | Tier | Planned wave | State |
|---|---|---|---|---|---|
| 1.1.1 | `pom.xml`, `src/main/java/com/pg/seo/domain/**`, `src/main/java/com/pg/seo/repository/**`, `src/main/resources/db/migration/**` | - | judgment | 1 | VERIFIED |
| 1.1.2 | `src/main/java/com/pg/seo/config/**`, `src/main/java/com/pg/seo/security/**`, `src/main/resources/application*.yml` | 1.1.1 | mechanical | 2 | VERIFIED |
| 1.1.3 | `src/main/java/com/pg/seo/service/**`, `src/main/java/com/pg/seo/mail/**` | 1.1.1 | judgment | 2 | VERIFIED |
| 1.1.4 | `src/main/java/com/pg/seo/web/**`, `src/main/java/com/pg/seo/extraction/**`, `src/main/java/com/pg/seo/scheduler/**` | 1.1.1, 1.1.2, 1.1.3 | judgment | 2 | VERIFIED |
| 1.1.5 | `src/main/resources/templates/**`, `src/main/resources/static/**` | 1.1.4 | judgment | 2 | VERIFIED |
| 1.2.1 | `src/test/**`, `tools/verify/**` | 1.1.5 | mechanical | 3 | VERIFIED |

## Status log

Append events to `.unlazy/seo-v01-rebuild/status.log`.

```text
node <skill-dir>/scripts/gate-check.mjs --scope seo-v01-rebuild --log "leaf-1.1.1 dispatched"
node <skill-dir>/scripts/gate-check.mjs --scope seo-v01-rebuild --log "leaf-1.1.1 verified"
node <skill-dir>/scripts/gate-check.mjs --scope seo-v01-rebuild --log "leaf-1.1.1 lease released"
```
