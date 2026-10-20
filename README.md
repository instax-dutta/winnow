# seo-0.1.0

A cold-email and lead-generation platform. `seo-0.1.0` is a clean-room rebuild of
`seo-0.0.1-SNAPSHOT`, a Spring Boot 3.3.0 application that was decompiled and analysed before
the rewrite. The rebuild keeps the product surface of the original and fixes every defect the
analysis found.

The original was a 93 MB fat JAR. Its behaviour is documented, defect by defect, in
`docs/reverse-engineering/`. That record is the ground truth for parity: a change that
alters what the original did has to be justified against it, so a fix is never an accidental
regression.

## Start here

**New to this project?** Read [`docs/EXPLAINER.html`](docs/EXPLAINER.html) - a plain-language
walkthrough of what the original application did, what was wrong with it, and what changed.
It opens straight from disk, no server needed.

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
│   ├── MIGRATION-FROM-0.0.1.md   Table-by-table and endpoint-by-endpoint mapping
│   ├── SECURITY.md               Threat model, controls, remaining limitations
│   ├── MAINTENANCE.md            How to build, deploy, migrate and troubleshoot
│   ├── SECRET-HARVEST.md         What the original leaked and the rotation order
│   └── reverse-engineering/      The full decompilation analysis of the original
├── reference/decompiled/         The decompiled original. Read-only evidence, never edit
├── src/main/java/com/pg/seo/     The application
├── src/main/resources/           Templates, static assets, Flyway migrations, config
├── src/test/                     The test suite. Hermetic: no network, no live mailbox.
└── tools/                        Repository tooling and the verification scripts
```

The Java packages under `src/main/java/com/pg/seo/` each have their own `AGENTS.md` with the
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

Builds run on the remote host `tejes@pelican`. `tools/sync-and-build.sh` mirrors the working
tree there and runs Maven on it:

```bash
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
- There is no licence, no activation and no machine fingerprinting. The original refused to
  authenticate unless the host's MAC address matched a value in its configuration, which was
  a licensing control dressed up as security. There is no `NetworkInterface` or
  `getHardwareAddress` call in this codebase, and `tools/verify/code-hygiene.mjs` fails the
  build if one reappears.
- `SECRETS.txt` in the repository root is git-ignored. It exists only so the owner can
  inspect what the original leaked.

### The original's credentials were exposed

The credentials of the original application were found to be exposed in the shipped artifact
and are documented in `docs/SECRET-HARVEST.md`: the MySQL root password, two Google OAuth
clients with live refresh tokens, 41 SMTP application passwords, a licence token and a
WordPress API token. **The owner of that installation should rotate all of them**, in the
order `docs/SECRET-HARVEST.md` gives. The rebuild does not carry any of them forward, but
nothing that was published stays secret.

## Further reading

- `docs/ARCHITECTURE.md` - how the system is built.
- `docs/MIGRATION-FROM-0.0.1.md` - what maps to what, and every behavioural change.
- `docs/SECURITY.md` - the threat model and the controls.
- `docs/MAINTENANCE.md` - the operating manual.
- `docs/reverse-engineering/` - the record of what the original actually did.
