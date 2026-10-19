# scheduler/

## Purpose

Scheduled work. Each job is a small trigger that claims work from a table and hands it to a
service. The scheduler package owns when work happens, never what the work does.

## Ownership

- `OutboxDispatchJob` - claims due outbox rows and dispatches them.
- `InboxPollJob` - polls configured mailboxes for new messages.
- `CampaignDispatchJob` - claims the queued campaigns and sends their recipients.
- `ExtractionJob` - advances long-running extraction runs.

## Local Contracts

- Jobs are idempotent. A job that runs twice does the work once. Work is claimed with a
  conditional status update, not by reading and then writing.
- The scheduler pool has more than one thread. The original used a single-threaded scheduler,
  so one stalled IMAP connection delayed every other job.
- A job that throws is logged and the job keeps running. One failure never stops the pool.
- A job never holds a database transaction open across a network call.
- Long work runs under a lock so two instances of the application do not both process the
  same rows.
- The schedule is configurable, and the default is documented in `application.yml`.

## Work Guidance

- A new job: name it, give it a schedule key in `application.yml`, make it idempotent, and
  give it a test that runs the job body twice and asserts the work happened once.
- Prefer a periodic trigger with a claim step over a long-running trigger.

## Public API

| Job | Method | Schedule key |
|---|---|---|
| `OutboxDispatchJob` | `dispatchDue()` | `seo.scheduler.outbox-delay-ms`, `seo.scheduler.outbox-batch-size` |
| `InboxPollJob` | `poll()` | `seo.scheduler.inbox-delay-ms` |
| `ExtractionJob` | `advance()` | `seo.scheduler.extraction-delay-ms`, `seo.scheduler.extraction-run-timeout-minutes` |

- `OutboxDispatchJob` claims the rows with `OutboxService.claimDue`, resolves the sending
  account of the row inside the tenant of the row and sends it with the nine argument
  `SendRequest`. On success the row is marked sent, on failure it is marked failed.
- `InboxPollJob` reads the mailbox of every enabled SMTP account over IMAP with an explicit
  connect, read and write timeout. A message is identified by the id the provider gave it,
  so a repeated poll stores it once.
- `ExtractionJob` closes every run that was left behind. A queued run is closed as failed, a
  running run that passed the run timeout is resumed through `ExtractionWorker`. A run is
  never left in `RUNNING`.

## Verification

- `./mvnw test` - the scheduler tests assert idempotency and that a failing job does not
  prevent the next one from running.

## Child DOX Index

This package has no child DOX files.
