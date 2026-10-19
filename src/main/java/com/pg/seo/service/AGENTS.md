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

## Work Guidance

- A service that fans work out to a pool bounds the pool, bounds the work, and reports
  progress. The original had unbounded fan-out with a six-hour timeout.
- Every bulk write is batched. No per-row `save()` in a loop.
- Every outbound call has an explicit timeout.

## Verification

- `./mvnw test` - each service has unit tests with a mocked provider, plus integration tests
  for the tenant-filter behaviour.

## Child DOX Index

This package has no child DOX files.
