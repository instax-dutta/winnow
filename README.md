# winnow

[![CI](https://github.com/instax-dutta/winnow/actions/workflows/ci.yml/badge.svg)](https://github.com/instax-dutta/winnow/actions/workflows/ci.yml)
[![CodeQL](https://github.com/instax-dutta/winnow/actions/workflows/codeql.yml/badge.svg)](https://github.com/instax-dutta/winnow/actions/workflows/codeql.yml)
[![License: AGPL-3.0](https://img.shields.io/badge/License-AGPL--3.0-blue.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/java-21-orange.svg)](https://openjdk.org/)

**Self-hosted lead generation for agencies. Your data stays on your machine, and your
mailing costs stay low.**

Winnow is a cold-mail and lead-generation platform. It finds contact addresses on the
websites you point it at, keeps them in lists you control, sends your own email from your
own mailboxes, and watches those mailboxes for replies. Nothing is sent to a third-party
service, so there is no per-lead fee and no third party holding your prospect list.

The name is the job: *to winnow* is to separate the valuable from the chaff.

Version 0.1.0. Spring Boot 3.5 on Java 21, MySQL 8, and a container image you can run
yourself.

## Start here

**New to this project?** Read [`docs/EXPLAINER.html`](docs/EXPLAINER.html) - a plain-language
walkthrough of the whole system: what it does, how the data flows, how to run it, and how
it is put together. It opens straight from disk, no server needed.

## What it does

An operator signs in, loads their own clients and client groups, writes an email template and
sends it through the approval workflow, registers the SMTP or Google OAuth accounts the mail
goes out from, and then runs a campaign against a group of contacts or a scheduled outbox of
single messages. Separately, the operator uploads a list of domains and the platform crawls
each one and pulls out the contact addresses it can find. An inbox monitor pulls the inbound
mail of the registered mailboxes.

## Features

- Client groups and client contacts, with a spreadsheet import.
- Email templates with an approval workflow: draft, submit, approve or reject, with a
  version number and the identity of the approver.
- SMTP sending accounts (host, port, protocol, auth method) and Google OAuth accounts, both
  holding only a reference to a credential that lives in the secret store.
- Campaigns against a group or against named contacts, with one delivery record per recipient
  carrying its own status and error. The campaign is recorded today; the job that dispatches
  its delivery rows is not implemented yet, and the scheduled outbox is the path that sends.
- A scheduled outbox for messages that are to go out at a chosen time, claimed with a
  conditional update so a row is never sent twice.
- Domain-to-email extraction: an uploaded domain list, an allow-list of fetchable hosts, a
  crawl, an address regex, a batched write and a run status.
- An inbox monitor that polls the registered mailboxes over IMAP.
- Four roles: administrator, manager, campaign operator, read-only viewer.
- Per-tenant data isolation: the signed-in user is the tenant, and every read and write of
  tenant-owned data filters on it.

## Quick start

Requirements: Docker with the Compose plugin, and roughly 1 GB of free memory.

```bash
docker compose up -d --build
docker compose logs -f app
```

The stack is two services. `db` is MySQL 8, published on `127.0.0.1:3307` only. `app` is the
application, published on `127.0.0.1:8083`. Flyway applies the schema on the first boot.

Open `http://127.0.0.1:8083/login` and sign in.

### Default sign-in

| Field | Value |
|---|---|
| Username | `admin` |
| Password | `change-me-now` |

That is the plaintext whose BCrypt hash (cost 12) is seeded by
`src/main/resources/db/migration/V1__baseline.sql`. It is a public default and is meant to be
changed before the application carries anything real.

To set the administrator password yourself, put it in the environment before the first start:

```bash
export BOOTSTRAP_PASSWORD='<choose-a-password>'
docker compose up -d --build
```

`seo.bootstrap.*` creates and activates the first administrator on a fresh install. It is a
deployment convenience. It is not a licence and it does not gate the application: with the
variable unset the application starts normally and leaves the seeded account alone.

## Configuration

Every value below is an environment variable. `application.yml` holds the defaults and holds
no secret.

| Variable | Default | What it controls |
|---|---|---|
| `DB_URL` | `jdbc:mysql://127.0.0.1:3307/emailsender_v01?...` | The JDBC URL. In Compose this is overridden to point at the `db` service. |
| `DB_USERNAME` | `seo_v01` | The database user. |
| `DB_PASSWORD` | (empty) | The database password. Set it in every deployment. The Compose file has a development default; override it. |
| `BOOTSTRAP_USERNAME` | `admin` | The account the bootstrap password is applied to. |
| `BOOTSTRAP_PASSWORD` | (empty) | The password applied to the bootstrap account on start. Empty means leave the account untouched. |
| `SECRET_STORE_PATH` | `./.secrets` | The directory the secret store keeps its files in. In the container it is `/app/.secrets`, on a volume. |
| `UPLOAD_DIR` | `./data/exports` | Where generated exports are written. In the container it is `/app/data`, on a volume. |
| `SERVER_PORT` | `8083` | The port the application listens on. |

The complete property surface, including the `seo.*` groups that have no environment
variable, is documented in `docs/ARCHITECTURE.md`.

## Project layout

```
.
├── AGENTS.md                     The DOX rail: the rules of this repository
├── GATES.md                      The verification ledger. Every gate has a command.
├── docker-compose.yml            The local two-service stack
├── Dockerfile                    Build stage and runtime stage
├── docs/
│   ├── AGENTS.md                 The documentation contract
│   ├── ARCHITECTURE.md           Layers, data model, flows, configuration, deployment
│   ├── EXPLAINER.html            A plain-language walkthrough of the whole system
│   ├── SECURITY.md               Threat model, controls, remaining limitations
│   └── MAINTENANCE.md            How to build, deploy, migrate and troubleshoot
├── src/main/java/io/github/instaxdutta/winnow/     The application
├── src/main/resources/           Templates, static assets, Flyway migrations, config
├── src/test/                     The test suite. Hermetic: no network, no live mailbox.
└── tools/                        Repository tooling and the verification scripts
```

The Java packages under `src/main/java/io/github/instaxdutta/winnow/` each have their own `AGENTS.md` with the
contract for that package. `docs/ARCHITECTURE.md` has the map.

## Build and test

Java 21 and the Maven wrapper. A system Maven is never needed.

```bash
./mvnw verify
```

`verify` is the gate: it compiles, runs the unit tests and the H2-backed integration tests,
and applies the Flyway migration to a clean schema. The test suite is hermetic. It opens no
network connection, reaches no live mailbox and starts no browser.

Other useful commands:

```bash
./mvnw clean compile             # compile only
./mvnw test                      # the test suite
./mvnw test -Dtest=ClassName     # one test class
./mvnw spring-boot:run           # run against a local MySQL
```

A self-hosted build host can be used instead, so that a build does not compete with the rest
of the work on your machine. `tools/sync-and-build.sh` mirrors the working tree there and
runs Maven on it:

```bash
export SEO_BUILD_HOST='<the-build-host>'
export SEO_BUILD_DIR='<the-directory-on-that-host>'
./tools/sync-and-build.sh verify
```

## Security note

- No credential is ever committed. The database schema holds a `secret_ref`, never a
  password, a client secret or a refresh token. The value lives in the secret store
  (`SECRET_STORE_PATH`), one file per secret, mode 0600 in a 0700 directory, named by the
  SHA-256 of the reference so the directory listing discloses nothing.
- No DTO and no response body ever carries a credential. An account DTO reports
  `hasCredential` and nothing else.
- No log line carries a credential, a token, a password hash or a session id.
- There is no licence, no activation and no machine fingerprinting. The application asks
  for a password and nothing else. There is no `NetworkInterface` or `getHardwareAddress`
  call in this codebase, and `tools/verify/code-hygiene.mjs` fails the build if one
  reappears.
- Every file in the repository is scanned for credential-shaped literals on every build. A
  match fails the build.

## Further reading

- `docs/EXPLAINER.html` - the whole system, in plain language.
- `docs/ARCHITECTURE.md` - how the system is built.
- `docs/SECURITY.md` - the threat model and the controls.
- `docs/MAINTENANCE.md` - the operating manual.
