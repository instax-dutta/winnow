# domain/

## Purpose

JPA entities, enums and the shared base types. This package is the model of the data, and it
knows nothing about how the data is fetched or presented.

## Ownership

- `AuditedEntity` - the base for every tenant-owned entity. It owns `id`, `ownerUserId`,
  `createdAt`, `createdBy`, `updatedAt`, `updatedBy`, `deletedAt`.
- `*Entity` classes - one per table, named after the table in PascalCase.
- Enums - every status, every type, every target kind. No free-text status columns.

## Local Contracts

- `LogSafe` lives here because it is the bottom of the stack: anything that logs a value
  from outside the system uses it, and it has to be reachable from every layer.
- **Every value that reaches a log from outside the system goes through `LogSafe`.** A
  newline in a value would split one log entry into two, and an escape sequence could make
  a log viewer do something unexpected. The sanitiser reduces the value to printable
  characters and caps its length.
- A value that came from the database, from a request, from a file upload or from the
  environment is outside the system. An identifier, a count and the class name of an
  exception are not.

- Extend `AuditedEntity` unless the entity is documented as global.
- Timestamps are `Instant`. The column is `DATETIME(6)`. No `String`, no `java.sql.Date`.
- Soft delete is `deletedAt`. Soft-deleted rows are invisible to default repository methods.
- Enum columns are `VARCHAR(32)` with a `CHECK` in the migration.
- A field holding or describing a credential is forbidden. The entity holds `secretRef`.
- `equals`/`hashCode` are based on the id only, never on the mutable business fields, and never
  on a collection. A generated `equals` that includes the child collection makes hash codes
  unstable - do not repeat that.
- Bidirectional relations are avoided. Where a relation is needed, the child holds the FK.

## Work Guidance

- Adding a field: update the entity and add a Flyway migration in the same change.
- Renaming a column: entity, migration and any native query together.
- Never add a `@Column` with a length that truncates a realistic value. `VARCHAR(255)` for
  full HTML email bodies and full email message content truncates them.

## Verification

- `./mvnw test` covers the mapping through the integration tests, which run Flyway against H2
  and then exercise the repositories.

## Child DOX Index

This package has no child DOX files.
