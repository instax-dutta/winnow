# DOX framework

- DOX is highly performant AGENTS.md hierarchy installed here
- Agent must follow DOX instructions across any edits

## Core Contract

- AGENTS.md files are binding work contracts for their subtrees
- Work products, source materials, instructions, records, assets, and durable docs must stay understandable from the nearest applicable AGENTS.md plus every parent AGENTS.md above it

## Read Before Editing

1. Read the root AGENTS.md
2. Identify every file or folder you expect to touch
3. Walk from the repository root to each target path
4. Read every AGENTS.md found along each route
5. If a parent AGENTS.md lists a child AGENTS.md whose scope contains the path, read that child and continue from there
6. Use the nearest AGENTS.md as the local contract and parent docs for repo-wide rules
7. If docs conflict, the closer doc controls local work details, but no child doc may weaken DOX

Do not rely on memory. Re-read the applicable DOX chain in the current session before editing.

## Update After Editing

Every meaningful change requires a DOX pass before the task is done.

Update the closest owning AGENTS.md when a change affects:

- purpose, scope, ownership, or responsibilities
- durable structure, contracts, workflows, or operating rules
- required inputs, outputs, permissions, constraints, side effects, or artifacts
- user preferences about behavior, communication, process, organization, or quality
- AGENTS.md creation, deletion, move, rename, or index contents

Update parent docs when parent-level structure, ownership, workflow, or child index changes. Update child docs when parent changes alter local rules. Remove stale or contradictory text immediately. Small edits that do not change behavior or contracts may leave docs unchanged, but the DOX pass still must happen.

## Hierarchy

- Root AGENTS.md is the DOX rail: project-wide instructions, global preferences, durable workflow rules, and the top-level Child DOX Index
- Child AGENTS.md files own domain-specific instructions and their own Child DOX Index
- Each parent explains what its direct children cover and what stays owned by the parent
- The closer a doc is to the work, the more specific and practical it must be

## Child Doc Shape

- Create a child AGENTS.md when a folder becomes a durable boundary with its own purpose, rules, responsibilities, workflow, materials, or quality standards
- Work Guidance must reflect the current standards of the project or user instructions; if there are no specific standards or instructions yet, leave it empty
- Verification must reflect an existing check; if no verification framework exists yet, leave it empty and update it when one exists

Default section order:
- Purpose
- Ownership
- Local Contracts
- Work Guidance
- Verification
- Child DOX Index

## Style

- Keep docs concise, current, and operational
- Document stable contracts, not diary entries
- Put broad rules in parent docs and concrete details in child docs
- Prefer direct bullets with explicit names
- Do not duplicate rules across many files unless each scope needs a local version
- Delete stale notes instead of explaining history
- Trim obvious statements, repeated rules, misplaced detail, and warnings for risks that no longer exist

## Closeout

1. Re-check changed paths against the DOX chain
2. Update nearest owning docs and any affected parents or children
3. Refresh every affected Child DOX Index
4. Remove stale or contradictory text
5. Run existing verification when relevant
6. Report any docs intentionally left unchanged and why

## Project

`seo-0.1.0` is a clean-room rebuild of the `seo-0.0.1-SNAPSHOT` cold-email / lead-generation
platform. The original is a Spring Boot 3.3.0 fat JAR (`com.pg.seo.EmailSenderApplication`) that
was decompiled into `reference/decompiled/` and analysed in `docs/reverse-engineering/`.

The rebuild keeps the original's product surface (clients, groups, templates, SMTP + Gmail
sending, scheduled outbox, domain-to-email extraction, inbox monitoring) and fixes every defect
found in the reverse engineering. Read `docs/reverse-engineering/` before changing behaviour
that the original defined - it is the record of what the original actually did, including the
bugs, so a change is never an accidental regression.

## Open Source

This project is intended for open source release. It carries **no licence gating of any
kind** and no activation mechanism:

- No machine fingerprinting. The original application refused to authenticate unless the
  host's MAC address matched a value in its config, which is a licensing control dressed up
  as security. There is no `NetworkInterface` or `getHardwareAddress` call in this codebase,
  and `tools/verify/code-hygiene.mjs` fails the build if one reappears.
- No licence token, licence key or activation check. The original shipped an HS256 JWT that
  it only ever *decoded* without verifying the signature, so it was trivially forgeable, and
  it expired on 2025-09-18 which locked the application out entirely. Neither the token nor
  the mechanism is carried over.
- No feature is held back behind a licence. Everything in this repository is released under
  the same terms.
- `seo.bootstrap.*` exists only to create the first administrator on a fresh install. It is
  a deployment convenience, not a licence.

Authentication and role-based authorization are NOT licensing. They stay.

## Stack Contract

- Java 21, Spring Boot 3.5.x, Maven (wrapper only - `mvnw`, never a system Maven).
- MySQL 8 for runtime, `utf8mb4_0900_ai_ci` everywhere. H2 in MySQL-compatibility mode for tests.
- Flyway owns the schema. No `hibernate.ddl-auto` in any profile except `test` (which is `none`).
- Thymeleaf for server-rendered pages, vanilla ES modules for the client side. No SPA framework.
- Never store a credential in the database, in a properties file, or in a git-tracked file.
  Credentials live in a secret manager and are referenced by key. See `docs/SECURITY.md`.

## Hard Rules

- **No secret in source.** No passwords, OAuth client secrets, refresh tokens, API keys or
  bearer tokens in any file that can be committed. `SECRETS.txt` is the one deliberate exception
  and it is git-ignored - it exists only so the owner can inspect what the original leaked.
- **Tenant isolation is not optional.** Every read and every write of tenant-owned data filters
  on the authenticated user's id. The original leaked other tenants' data on 11 endpoints;
  the rebuild must not reproduce that. New endpoints get a tenant filter by default.
- **State changes are not GET.** No `@GetMapping` may mutate state.
- **No `printStackTrace()`, no `System.out.println()` in application code.** Use the logger.
- **No `SimpleDateFormat` as a shared field.** Use `java.time` throughout.
- **No unbounded loops over user input.** Batch DB writes, bound the thread pool, bound the
  request timeout, and set an explicit timeout on every outbound HTTP call.
- **No `findAll()` on a tenant-owned table.** Scope by tenant or paginate.
- **Every outbound URL derived from user input is validated** against an allow-list. The
  original let a spreadsheet cell become an arbitrary `https://` fetch.

## Build And Run

```bash
./mvnw verify                 # build + full test suite (the gate)
./mvnw spring-boot:run        # run against the local dev MySQL
```

Builds run on `tejes@pelican` (Linux, 12 vCPU, Java 21). See `docs/MAINTENANCE.md` for the
remote build layout, the dedicated MySQL container, and the rule that nothing else running on
that host may be touched.

## Verification

`GATES.md` is the authoritative ledger. It is an unlazy ledger: every runnable gate has a
`CHECK:` command and an `EXPECT:` token, and a gate counts as met only when the command exits
zero and the expectation matches.

```bash
node ~/.agents/skills/unlazy/scripts/gate-lint.mjs GATES.md      # lint before working
node ~/.agents/skills/unlazy/scripts/gate-check.mjs --status GATES.md
node ~/.agents/skills/unlazy/scripts/gate-check.mjs --approve GATES.md   # only for commands you wrote
```

## User Preferences

- Never use emojis in code, docs or commits.
- Use dashes, not em dashes.
- When making technical decisions, do not over-weight development cost.
- For bug fixes, reproduce the bug end to end as a user would experience it first.
- Commit results, not just code: measured numbers belong in the repo with their provenance.

## Child DOX Index

| Path | Scope |
|---|---|
| `docs/` | Architecture, migration notes, security model, reverse-engineering record |
| `docs/reverse-engineering/` | Full decompilation analysis of seo-0.0.1-SNAPSHOT (the ground truth for parity) |
| `reference/` | Decompiled original sources - read-only evidence, never edit |
| `tools/` | Repository tooling (secret harvester and future dev scripts) |
| `src/` | Application source, resources and tests |
