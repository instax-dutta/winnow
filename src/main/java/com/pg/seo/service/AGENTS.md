# service/

## Purpose

Business logic and transaction boundaries. A service owns a use case end to end and is the
layer where the tenant filter is applied, not merely forwarded.

## Ownership

| Service | Owns |
|---|---|
| `ClientGroupService` | client groups and their lifecycle |
| `ClientService` | client contacts, Excel import |
| `TemplateService` | email templates and the approval workflow |
| `SmtpAccountService` | SMTP sending accounts, credential references |
| `GmailAccountService` | Google OAuth accounts, credential references |
| `CampaignService` | creating a send job, expanding recipients, recording delivery |
| `OutboxService` | the scheduled outbox and its dispatch |
| `ExtractionService` | extraction runs and their results |
| `InboxService` | inbound message capture |
| `DashboardService` | aggregate counters for the dashboard |
| `UserService` | the caller's own account and its password |
| `DomainCrawler` | the port the extraction package implements |

## Local Contracts

- A service method takes the owner id explicitly. It never reads it from a thread-local, a
  session, or a request-scoped bean. The web layer resolves the principal and passes it down.
- `@Transactional` is declared on the service method, not on the repository.
- A method that writes more than one row declares a transaction.
- Secrets are resolved inside the service and passed to the provider adapter. They are never
  returned, never stored on an entity, never logged.
- No service returns a JPA entity directly to the web layer. It returns a DTO.
- No service calls a provider (SMTP, Gmail, HTTP) while holding a database transaction open
  longer than the call. Long sends run outside the transaction and report back by id.
- The one exception is a claim, which has to be a single statement to stay correct. The
  outbox claim is a conditional update in `OutboxService`; it belongs in
  `OutboxRepository` next to `findClaimable`, which does not expose a conditional update
  yet.

## Work Guidance

- A service that fans work out to a pool bounds the pool, bounds the work, and reports
  progress. The original had unbounded fan-out with a six-hour timeout.
- Every bulk write is batched. No per-row `save()` in a loop.
- Every outbound call has an explicit timeout.
- A credential in a request record is write only. The service stores it in the secret store
  and the DTO that comes back carries a `hasCredential` flag and nothing else.
- `DomainCrawler` is the only way the service layer reaches the extraction package. The
  extraction package implements it, the service layer calls it.

## Public API

Every method takes the owner id first and returns a DTO.

| Type | Methods |
|---|---|
| `ClientGroupService` | `list`, `create`, `update`, `delete` |
| `ClientService` | `list`, `importExcel`, `delete`, `resolveTargets` |
| `TemplateService` | `list`, `create`, `update`, `delete`, `submitForApproval`, `listPendingApproval`, `approve`, `reject` |
| `SmtpAccountService` | `list`, `create`, `update`, `delete` |
| `GmailAccountService` | `list`, `create`, `update`, `delete` |
| `CampaignService` | `create`, `list`, `get`, `deliveries`, `cancel` |
| `OutboxService` | `schedule`, `list`, `cancel`, `claimDue`, `markSent`, `markFailed` |
| `ExtractionService` | `start`, `get`, `results`, `list` |
| `InboxService` | `list`, `unreadCount`, `count` |
| `DashboardService` | `summary` |
| `UserService` | `current`, `list`, `changePassword` |
| `DomainCrawler` | `findEmails(String domain)` |

- `OutboxService.claimDue(Instant now, int limit)` returns the claimed rows. It is the
  dispatch primitive the scheduler package calls, together with `markSent` and `markFailed`.
- `ExtractionService.start` reads the domains from column A, creates the run and hands the
  domains to the `DomainCrawler`. It is deliberately not transactional: the crawl is an
  outbound call and must not hold a transaction open.
- `TemplateService.update` bumps the version and sends an edited template back to `DRAFT`,
  so an approved template cannot be sent in a form nobody approved.

## Verification

- `./mvnw test` - each service has unit tests with a mocked provider, plus integration tests
  for the tenant-filter behaviour.

## Child DOX Index

This package has no child DOX files.
