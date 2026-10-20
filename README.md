<div align="center">

# winnow

**Self-hosted lead generation for agencies. Your prospect list stays on your machine, and
your cost per lead stays at zero.**

[![CI](https://github.com/instax-dutta/winnow/actions/workflows/ci.yml/badge.svg)](https://github.com/instax-dutta/winnow/actions/workflows/ci.yml)
[![CodeQL](https://github.com/instax-dutta/winnow/actions/workflows/codeql.yml/badge.svg)](https://github.com/instax-dutta/winnow/actions/workflows/codeql.yml)
[![Release](https://img.shields.io/github/v/release/instax-dutta/winnow?label=release)](https://github.com/instax-dutta/winnow/releases/latest)
[![License: AGPL-3.0](https://img.shields.io/badge/License-AGPL--3.0-2f6f4e.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/java-21-e76f00.svg)](https://openjdk.org/)

</div>

---

## The problem

Most lead generation tools want two things from you: your prospect list, and a per-lead fee.

The list is the asset. It is the pipeline you will work for a year, the contacts you will
email again next quarter, the buyers who will come back. Handing it to a vendor means
handing it to their vendors, their backups, their log processors and their churn.

The fee compounds the problem. A tool that charges per lead quietly tells you not to look
too hard at whether a lead was worth paying for. And at the scale where a campaign starts
paying, the bill is the size of a person.

Meanwhile the work itself is not hard. Visit a list of websites, read out the contact
addresses. Write an email, send it from the mailboxes you already pay for. Watch for
replies. There is nothing here that needs a vendor.

**winnow is that work, running on your own hardware.** One command brings it up. The list
never leaves the machine. There is no per-lead meter anywhere in it.

## What you get

Four jobs, and nothing else. That is deliberate: every feature that is not one of these
four is a feature you would have to learn, configure, pay for and trust.

| | |
|---|---|
| **Find** | Upload a list of websites. winnow visits each one, reads the page and pulls out the contact addresses it can find, deduplicated and exportable. |
| **Organise** | Those addresses become clients. Sort them into named groups so a campaign goes to the right list and not the wrong one. |
| **Send** | Write an email once, in rich text, and get it approved. Then send it to a group or to named contacts, from the mailboxes you already own. Every recipient gets their own delivery record. |
| **Catch replies** | winnow polls your mailboxes and files the replies where you can see who answered and what they said. |

<div align="center">

**Find** &nbsp;→&nbsp; **Organise** &nbsp;→&nbsp; **Send** &nbsp;→&nbsp; **Catch replies**

</div>

## How it works

```
   you                    winnow                       the internet
    |                        |                              |
    |  upload a domain list  |                              |
    |----------------------->|  check the host is safe    |
    |                        |  visit, read, extract      |
    |                        |---------------------------->|
    |                        |  addresses found           |
    |                        |<----------------------------|
    |  your groups, your     |                              |
    |  templates, your       |  send from your mailboxes   |
    |  mailboxes             |---------------------->      |
    |                        |                              |
    |  the replies           |  poll, read, file           |
    |<-----------------------|  every few minutes          |
```

The interesting line is the second one. winnow refuses to fetch a host that points at a
private network, a loopback interface or a cloud metadata address, and it re-checks every
redirect. A spreadsheet cell can never become an arbitrary request.

## What makes it different

**Your list is not an asset of someone else's business.** Tenants are isolated by
construction: the signed-in user is the tenant, the tenant id is passed explicitly into
every service call, and the database filters on it again. 75 of the automated checks exist to
prove that one tenant can never read or change another tenant's data.

**Your credentials are not in the database.** Not encrypted in it, not hashed in it, not in
it. A mail account row holds a *reference*; the secret itself lives in a protected store
and is fetched for the one send that needs it. No API response, no log line and no file in
this repository ever contains one. A check scans every file in the project on every build
and fails if a credential-shaped literal appears.

**Your cost does not move when your lead volume does.** The mailboxes are the ones you
already pay for. winnow is software, and software is the same price at ten thousand leads as
at ten.

**It does not phone home.** No telemetry, no usage reporting, no account registration, no
licence server, no activation. Start it and it runs. There is no key to expire, no machine
to be bound to, and no account to lose access to.

## Who it is for

- **Agencies and consultancies** running outbound campaigns, where the list is worth more
  than the tooling.
- **Teams that already send cold mail** and want the boring half of it to be reliable
  rather than clever.
- **Anyone whose prospect list is sensitive** and who would rather not hand it to a
  vendor to find out later that they did.

## Who it is not for

Being clear about this saves everyone time.

- **Not a marketing automation suite.** There is no drip-sequence builder, no lead
  scoring, no CRM, no attribution dashboard, no subject-line A/B testing.
- **Not a deliverability tool.** No inbox warming, no send-pace optimisation, no placement
  prediction. You manage the reputation of your own mailboxes.
- **Not a data broker.** It finds addresses on the domains *you* name, for *your* use. It
  does not aggregate, sell, licence or enrich a list for anyone else.
- **Not a hosted service, and not heading that way.** There is no multi-tenant hosting
  tier, no billing and no metering. Self-hosting is the product.
- **Not a mail server.** It sends through your mailboxes. It does not run one.

## Get it running

You need Docker. That is the whole prerequisite list.

```bash
git clone https://github.com/instax-dutta/winnow
cd winnow
cp .env.development.example .env.development   # set one password
docker compose up -d --build
```

Open `http://127.0.0.1:8083`. Sign in as `admin` with the password you set.

That is the whole setup. The database creates itself, the schema migrates itself, and the
first administrator is created from the environment variable you set. There is no wizard and
no account to register for.

<details>
<summary>Run it from a release instead</summary>

```bash
docker compose up -d          # uses the published image
```

or, with a MySQL 8 you already have:

```bash
DB_URL='jdbc:mysql://host:3306/emailsender?useSSL=false&serverTimezone=UTC' \
DB_USERNAME=you DB_PASSWORD=your-password \
BOOTSTRAP_PASSWORD='<choose-a-password>' \
java -jar winnow-*.jar
```

Every release carries the jar, a `SHA256SUMS` file and a software bill of materials, and
the build is attested. Verify it before you trust it:

```bash
gh attestation verify winnow-*.jar --repo instax-dutta/winnow
```

</details>

<details>
<summary>Build it from source</summary>

You need a JDK 21. The Maven wrapper is in the repository, so there is nothing to install.

```bash
./mvnw verify                 # compile, run every test, run the checks
./mvnw spring-boot:run        # run against a database you already have
```

`./mvnw verify` is what the build server runs. It is the same thing your change has to
pass.

</details>

## Configuration

Everything is read from the environment, so the same image runs in every place.

| Variable | What it does | Default |
|---|---|---|
| `DB_URL` | Where the database is. | the compose database |
| `DB_USERNAME` / `DB_PASSWORD` | How to reach it. | `seo_v01` |
| `BOOTSTRAP_USERNAME` / `BOOTSTRAP_PASSWORD` | Creates the first administrator on a fresh install. **Change it.** | `admin` / empty |
| `SECRET_STORE_PATH` | Where credentials are kept. On a cluster, every instance must mount the same volume here. | `./.secrets` |
| `SECRET_STORE_TYPE` | `file`, or the name of a contributed store. | `file` |
| `UPLOAD_DIR` | Where generated spreadsheets are written. | `./data/exports` |
| `SERVER_PORT` | Which port the application listens on. | `8083` |
| `SCRAPER_URL` | Where the optional Amazon sidecar is running. | `http://scraper:3000` |

Full list in `docs/MAINTENANCE.md`.

## How it protects you

The controls are the product, not a footer. Every one of these is enforced by code and
checked by a build that fails when it is broken.

| | |
|---|---|
| **Tenancy** | The signed-in user is the tenant. It is passed explicitly into every service method and filtered again in the database. 75 automated checks assert the isolation. |
| **Credentials** | Never in the database, never in a response, never in a log, never in a commit. The store holds one file per secret, mode 0600, named by a hash. |
| **Authentication** | bcrypt at cost 12, one live session per account, sessions evicted on expiry, lockout after repeated failures, one generic failure message so accounts cannot be enumerated. |
| **CSRF** | Enabled. Every state change is a POST, so a page load can never delete anything. |
| **SSRF** | Every outbound address is checked before the request. Private ranges are always refused and the refusal cannot be switched off. Redirects are re-checked. |
| **XSS** | User text is escaped by default. The two fields that carry HTML are sanitised on write. |
| **Log injection** | Every externally sourced value is reduced to printable characters before it reaches a log. |
| **Supply chain** | Every action is pinned to a commit SHA, Dependabot watches the dependencies, and secret scanning with push protection is on. |

`docs/SECURITY.md` has the threat model, the controls and the limits.

## What is honest about it

This is a young project and you should know that before you rely on it.

- **It is young.** It is pre-1.0. The interfaces will move. Read
  [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) before you build on it, and pin your
  version.
- **The Amazon sidecar is optional and separate.** It drives a real browser, which is the
  most fragile part of the system. Everything else works without it.
- **The credential store is a directory of files.** That is right for one host and for a
  cluster on a shared volume. It is not a key management service. If you need one, the
  seam is `SecretStore` and a contributor can add it.
- **Sending is ordinary SMTP and the Gmail API.** It does not solve deliverability, and it
  does not try to.
- **The extraction reads public pages.** It does not log in, work around a block, or
  respect a site's wishes beyond what its own terms say. That part is on you and on your
  legal advice.

## Built with, and checked by

The build is not a formality. There is a verification ledger, `GATES.md`, with fourteen
checks, and every one of them has to pass before a change lands:

```
SECRET-SCAN-PASS   no credential-shaped literal in any file
HYGIENE-PASS       no state change over GET, no shared date formatter, no licence gating
DOX-TREE-PASS      every folder's contract is present and consistent
PARITY-PASS        no reference to removed material anywhere in the tree
DOCS-PASS          the documentation set is complete
LINK-PASS          every link in the public documents resolves
MIGRATIONS-PASS    the schema applies to a clean MySQL 8
APP-BOOT-PASS      the container starts, serves and signs an administrator in
SCRAPER-PASS       the sidecar keeps its contract and its security properties
```

Plus 218 tests, CodeQL on every push, and a release pipeline that produces an attested
artifact with a bill of materials.

## Who's behind it

Built by people who run outbound campaigns and got tired of the tooling.

This is a community project and it is early. If you run agencies, run outbound, or just
want to own your lead list, it is open source under the AGPL-3.0 and contributions are
welcome, especially in the two areas we have marked as open in
[`CONTRIBUTING.md`](CONTRIBUTING.md).

## Read more

| | |
|---|---|
| [`docs/EXPLAINER.html`](docs/EXPLAINER.html) | The whole system in plain language. Open it straight from disk. |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | How it is put together, and why. |
| [`docs/SECURITY.md`](docs/SECURITY.md) | The threat model, the controls, the limits. |
| [`docs/MAINTENANCE.md`](docs/MAINTENANCE.md) | How to run, migrate and troubleshoot it. |
| [`AGENTS.md`](AGENTS.md) | The contract any developer or agent has to follow here. |

## Licence

AGPL-3.0. See [`LICENSE`](LICENSE).

If you want to run this for clients and you need different terms, ask.
