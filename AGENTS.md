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

## What this project is for

`winnow` exists so that an agency can find better leads, keep its prospect data private, and
keep its mailing costs low. Everything below follows from those three things.

- **Better leads.** The system has to produce a contact list an operator can actually work
  from: addresses that were really published on the domain, deduplicated, exportable, and
  attached to a named campaign so the reply can be traced back to the source.
- **Privacy.** A prospect list is a business asset. It lives on the operator's own machine.
  Credentials are never in the database, a response body, a log line or a committed file. One
  tenant can never see another's data. Nothing is sent to a third-party service.
- **Low cost.** The operator already pays for the mailboxes, so the sending costs nothing
  per lead. There is no per-lead fee anywhere in the design, and no third-party API in the
  path of a send.

## What this project is not

Stating the boundary is as important as stating the goal, because the most likely way to
ruin this project is to grow it past what it is.

- **It is not a marketing automation suite.** There is no drip sequence builder, no lead
  scoring, no CRM, no attribution dashboard, no A/B testing of subject lines.
- **It is not a deliverability tool.** There is no warm-up, no sending-pace optimisation, no
  inbox placement prediction. The operator manages the reputation of their own mailboxes.
- **It is not a data broker.** It finds addresses on the domains the operator names, for the
  operator's own use. It does not aggregate, sell, license or enrich a list for anyone else.
- **It does not scrape what a site forbids.** It reads public pages. It does not log in, work
  around a block, or defeat a rate limit.
- **It is not a hosted service.** There is no multi-tenant hosting tier, no billing, no usage
  metering, and no reason to add one. Self-hosting is the product.

A change that serves one of those three goals is welcome. A change that adds a fourth product
is not, whatever it is called.

## Project

`winnow` is a self-hosted cold-email and lead-generation platform for agencies. An operator
signs in, keeps their own clients and client groups, writes an email template and sends it
through an approval workflow, registers the SMTP or Google OAuth accounts the mail goes out
from, runs a campaign against a group of contacts or against a scheduled outbox of single
messages, uploads a list of domains to crawl for contact addresses, and watches the
registered mailboxes for replies.

Everything runs on the operator's own machine or their own server. No lead list, no mailbox
and no campaign ever reaches a third-party service, so there is no per-lead fee and no third
party holding the prospect list.

## Open Source

This project is open source and carries **no licence gating of any kind** and no activation
mechanism:

- No machine fingerprinting. There is no `NetworkInterface` or `getHardwareAddress` call in
  this codebase, and `tools/verify/code-hygiene.mjs` fails the build if one reappears.
- No licence token, licence key or activation check.
- No feature is held back. Everything in this repository is released under the same terms.
- `seo.bootstrap.*` exists only to create the first administrator on a fresh install. It is
  a deployment convenience, not an activation mechanism.

Authentication and role-based authorization are not licensing. They stay.

## Stack Contract

- Java 21, Spring Boot 3.5.x, Maven (wrapper only - `mvnw`, never a system Maven).
- MySQL 8 for runtime, `utf8mb4_0900_ai_ci` everywhere. H2 in MySQL-compatibility mode for tests.
- Flyway owns the schema. No `hibernate.ddl-auto` in any profile except `test` (which is `none`).
- Thymeleaf for server-rendered pages, vanilla ES modules for the client side. No SPA framework.
- Never store a credential in the database, in a properties file, or in a git-tracked file.
  Credentials live in a secret manager and are referenced by key. See `docs/SECURITY.md`.

## Hard Rules

- **No secret in source.** No passwords, OAuth client secrets, refresh tokens, API keys or
  bearer tokens in any file that can be committed. The credential inventory in the working
  tree is git-ignored and is never committed.
- **Tenant isolation is not optional.** Every read and every write of tenant-owned data filters
  on the authenticated user's id. One tenant must never see or change another tenant's data.
  New endpoints get a tenant filter by default.
- **State changes are not GET.** No `@GetMapping` may mutate state.
- **No `printStackTrace()`, no `System.out.println()` in application code.** Use the logger.
- **No `SimpleDateFormat` as a shared field.** Use `java.time` throughout.
- **No unbounded loops over user input.** Batch DB writes, bound the thread pool, bound the
  request timeout, and set an explicit timeout on every outbound HTTP call.
- **No `findAll()` on a tenant-owned table.** Scope by tenant or paginate.
- **Every outbound URL derived from user input is validated** against an allow-list. A
  spreadsheet cell must never become an arbitrary request.

## Build And Run

```bash
./mvnw verify                 # build + full test suite (the gate)
./mvnw spring-boot:run        # run against the local dev MySQL
```

A build host can be used instead of a local machine. Point `SEO_BUILD_HOST` at it and the
build scripts will run there. See `docs/MAINTENANCE.md`.

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
| `docs/` | Architecture, the plain-language walkthrough, the security model, the operating manual |
| `scraper/` | The Amazon search sidecar: a small Node service that drives a browser |
| `tools/` | Repository tooling: the verification scripts and the build helpers |
| `src/` | Application source, resources and tests |
