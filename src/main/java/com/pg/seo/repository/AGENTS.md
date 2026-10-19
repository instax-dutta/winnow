# repository/

## Purpose

Spring Data JPA repositories, one per aggregate. This package is the only place that writes
SQL, and it is where the tenant filter is enforced at the data-access level.

## Ownership

- `ClientGroupRepository` - client groups.
- `ClientRepository` - client contacts.
- `TemplateRepository` - email templates.
- `SmtpAccountRepository` - SMTP sending accounts.
- `GmailAccountRepository` - Google OAuth accounts.
- `CampaignRepository` - send jobs.
- `DeliveryRepository` - per-recipient delivery records.
- `OutboxRepository` - the scheduled outbox.
- `ExtractionRunRepository` - extraction runs.
- `ExtractionResultRepository` - extracted addresses.
- `InboxMessageRepository` - captured inbound messages.
- `UserRepository` - application users.
- `RoleRepository` - roles.

## Local Contracts

- Every method that reads or writes tenant-owned data takes the owner id. A method that does
  not take it is either a global-entity method (documented in `docs/ARCHITECTURE.md`) or a bug.
- Method names state the filter: `findByOwnerUserIdAndDeletedAtIsNull`, not `findAll`.
- Soft-deleted rows are excluded by default. A method that needs them says so in its name.
- No `select *` native queries. Native SQL needs a comment naming the reason a JPQL query
  could not express it.
- No method may be called with a null owner id. Services resolve the principal first.
- `findAll()` is forbidden on a tenant-owned table.

## Work Guidance

- Prefer a derived query. Use `@Query` when a derived name becomes unreadable.
- Aggregations that return counts belong here, named `countBy...`.
- A new query that will run on a large table needs the matching index in the migration.

## Verification

- `./mvnw test` - the integration tests assert that a tenant cannot read another tenant's rows
  through any repository method.

## Child DOX Index

This package has no child DOX files.
