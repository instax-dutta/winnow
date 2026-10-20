# Plan: winnow 0.1.0

Scope: winnow-0.1.0
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
| 1.1.1 | `pom.xml`, `src/main/java/io/github/instaxdutta/winnow/domain/**`, `src/main/java/io/github/instaxdutta/winnow/repository/**`, `src/main/resources/db/migration/**` |
| 1.1.2 | `src/main/java/io/github/instaxdutta/winnow/config/**`, `src/main/java/io/github/instaxdutta/winnow/security/**`, `src/main/resources/application*.yml` |
| 1.1.3 | `src/main/java/io/github/instaxdutta/winnow/service/**`, `src/main/java/io/github/instaxdutta/winnow/mail/**` |
| 1.1.4 | `src/main/java/io/github/instaxdutta/winnow/web/**`, `src/main/java/io/github/instaxdutta/winnow/extraction/**`, `src/main/java/io/github/instaxdutta/winnow/scheduler/**` |
| 1.1.5 | `src/main/resources/templates/**`, `src/main/resources/static/**` |
| 1.2.1 | `src/test/**`, `tools/verify/**` |

**Dependencies** - 1.1.2, 1.1.3, 1.1.4, 1.1.5 each need the domain and repository types from
1.1.1. 1.2.1 needs everything.

**Host launch mode** - the leaves are dispatched in the wave order below, with a sequential
fallback when the host cannot run them side by side.

**Wave policy** - wave 1: 1.1.1 (foundation, judged by the orchestrator because the whole
data model hangs off it). wave 2: 1.1.2, 1.1.3, 1.1.4, 1.1.5 in parallel, each with a disjoint
ownership set. wave 3: 1.2.1 (tests) after wave 2. Maximum host concurrency 4.

**Toolchain** - Java 21, Maven wrapper only (`./mvnw`), Node 20+ for the verify scripts, MySQL 8
in the `seo-v01-db` container on `127.0.0.1:3307`. The build working directory is the
directory named by `SEO_BUILD_DIR` on the host named by `SEO_BUILD_HOST`. Nothing else on that
host may be started, stopped or reconfigured.

**Conventions** - `java.time` only, no `SimpleDateFormat`. No `printStackTrace`, no
`System.out`. Lombok for entities and DTOs. Package-private where possible. Responses are
DTOs, never entities.

**Manual review** - the orchestrator reviews every leaf's diff against the DOX contracts in
`src/main/java/io/github/instaxdutta/winnow/AGENTS.md` and its package AGENTS.md before the leaf is verified.

## Current contract inventory

Contract revision: 1.

| ID | Required outcome or constraint | Owner | Observing gate | Disposition | Revision |
|---|---|---|---|---|---|
| C1 | The layering rule and the tenant model are written down for every package | 1.1.1 | G6, G7 | ACTIVE | 1 |
| C2 | No credential-shaped literal exists in the tracked tree | 1.2.1 | G10 | ACTIVE | 1 |
| C3 | No tracked file refers to an earlier version of the software or to removed material | 1.2.1 | G12 | ACTIVE | 1 |
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
| C15 | The build runs on the build host without disturbing anything else on it | orchestrator | G1, G13 | ACTIVE | 1 |

## State vocabulary

Leaf state is exactly one of WAITING, READY, IN-FLIGHT, VERIFIED, ABANDONED.
Branch state is exactly one of OPEN, VERIFIED, ABANDONED.

## Tree

- 1 winnow 0.1.0 .................. GATES.md
  - 1.1 implementation ............. the package contracts under src/main/java/io/github/instaxdutta/winnow
    - 1.1.1 data foundation ........ pom.xml, domain, repository, migrations
    - 1.1.2 config and security .... config, security, application.yml
    - 1.1.3 services and mail ...... service, mail
    - 1.1.4 web, extraction, jobs ... web, extraction, scheduler
    - 1.1.5 templates and assets .... templates and static assets
  - 1.2 verification ............... src/test and tools/verify
    - 1.2.1 tests and verify tools . src/test, tools/verify

## Leaf dispatch table

| Leaf | Owns | Needs | Tier | Planned wave | State |
|---|---|---|---|---|---|
| 1.1.1 | `pom.xml`, `src/main/java/io/github/instaxdutta/winnow/domain/**`, `src/main/java/io/github/instaxdutta/winnow/repository/**`, `src/main/resources/db/migration/**` | - | judgment | 1 | VERIFIED |
| 1.1.2 | `src/main/java/io/github/instaxdutta/winnow/config/**`, `src/main/java/io/github/instaxdutta/winnow/security/**`, `src/main/resources/application*.yml` | 1.1.1 | mechanical | 2 | VERIFIED |
| 1.1.3 | `src/main/java/io/github/instaxdutta/winnow/service/**`, `src/main/java/io/github/instaxdutta/winnow/mail/**` | 1.1.1 | judgment | 2 | VERIFIED |
| 1.1.4 | `src/main/java/io/github/instaxdutta/winnow/web/**`, `src/main/java/io/github/instaxdutta/winnow/extraction/**`, `src/main/java/io/github/instaxdutta/winnow/scheduler/**` | 1.1.1, 1.1.2, 1.1.3 | judgment | 2 | VERIFIED |
| 1.1.5 | `src/main/resources/templates/**`, `src/main/resources/static/**` | 1.1.4 | judgment | 2 | VERIFIED |
| 1.2.1 | `src/test/**`, `tools/verify/**` | 1.1.5 | mechanical | 3 | VERIFIED |

## Status log

Append events to `.unlazy/status.log`.

```text
node <skill-dir>/scripts/gate-check.mjs --scope winnow-0.1.0 --log "leaf-1.1.1 dispatched"
node <skill-dir>/scripts/gate-check.mjs --scope winnow-0.1.0 --log "leaf-1.1.1 verified"
node <skill-dir>/scripts/gate-check.mjs --scope winnow-0.1.0 --log "leaf-1.1.1 lease released"
```

## Outcome

Every leaf was implemented, reviewed against its DOX contract, and verified by the parent
re-verification. The root ledger `GATES.md` reports **13 of 13 gates met**, each with
recorded automatic evidence (`exit=0; EXPECT=matched`).

The measured results, at the time of the run:

| Gate | Outcome | What it measured |
|---|---|---|
| G1 | met | the project compiles from a clean checkout on the build host |
| G2 | met | the whole suite passes: 197 tests, 0 failures, 0 errors, 0 skipped |
| G3 | met | the containerised application starts, serves the login form, signs an administrator in and rejects a wrong password |
| G4 | met | 218 files scanned, 0 credential literals |
| G5 | met | 150 Java files, 0 rule violations, including the no-licence-gating rule |
| G6 | met | 19 AGENTS.md files, sections and index consistent |
| G7 | met | 75 tenant isolation tests pass |
| G8 | met | the migrations apply to a clean MySQL 8: 14 tables, every expected column, 16 foreign keys, 9 unique indexes, utf8mb4 throughout, no credential column |
| G9 | met | 49 CSRF tests pass, covering every mutating endpoint |
| G10 | met | the tracked tree carries no credential-shaped literal |
| G11 | met | 6 documents present and complete |
| G12 | met | the tracked tree refers to no earlier version of the software and to no removed material |
| G13 | met | a fresh copy with an empty Maven repository builds and produces the 88 MB jar |

Two defects were found by the tests during the run and fixed at the source, not in the test:
the three tables that were missing the audit columns their entities carried, and the
`@NotBlank` annotation on an enum field, which Hibernate Validator cannot handle.
