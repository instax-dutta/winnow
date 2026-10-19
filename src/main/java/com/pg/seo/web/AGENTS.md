# web/

## Purpose

The HTTP layer. It translates HTTP into a service call and a service result back into HTTP.
It holds no business rules and touches no repository directly.

## Ownership

- `@RestController` classes - one per resource area.
- `ApiResponse<T>` - the response envelope every JSON endpoint returns.
- `GlobalExceptionHandler` - maps domain exceptions to status codes.
- `PageController` - the server rendered pages.
- `UploadGuard` - the type and size check of an uploaded file.
- `Roles` - the role expressions the endpoints use.
- `AuthPrincipal` resolution helper - turns the Spring Security principal into the owner id
  and the set of authorities.

## Local Contracts

- The tenant comes from the authenticated principal. A request parameter can never choose it.
- No `@GetMapping` changes state. Deletes and mutations are `@PostMapping`, `@PutMapping` or
  `@DeleteMapping`. The original had six state-changing GET endpoints.
- Every mutating endpoint requires the CSRF token, which the front end sends as
  `X-CSRF-TOKEN` from the `_csrf` meta tag.
- The response envelope is always `ApiResponse`. A raw entity is never returned to the client.
- A DTO never carries a secret. An entity with a credential is never serialized. If an entity
  could be serialized at all, its secret fields carry `@JsonIgnore`.
- File uploads are parsed in memory, validated for type and size at the edge, and never
  written to a path built from user input.
- Paginated endpoints return `PagedResponse<T>` with a real total count.

## Endpoints

The tenant is resolved from `AuthPrincipalResolver` in every endpoint.

| Method | Path | Answer | Role |
|---|---|---|---|
| GET | `/login` | page `login` | the security package owns it |
| GET | `/dashboard` | page `dashboard`, with the counters as the model attribute `dashboard` | authenticated |
| GET | `/clients` | page `clients` | authenticated |
| GET | `/groups` | page `client-groups` | authenticated |
| GET | `/templates` | page `templates` | authenticated |
| GET | `/templates/approvals` | page `template-approvals` | authenticated |
| GET | `/accounts/smtp` | page `smtp-accounts` | authenticated |
| GET | `/accounts/gmail` | page `gmail-accounts` | authenticated |
| GET | `/campaigns` | page `campaigns` | authenticated |
| GET | `/outbox` | page `outbox` | authenticated |
| GET | `/extraction` | page `extraction` | authenticated |
| GET | `/inbox` | page `inbox` | authenticated |
| GET | `/api/clients` | `PagedResponse<ClientDto>` | authenticated |
| POST | `/api/clients/import` | `ClientImportResult` | admin, manager, operator |
| DELETE | `/api/clients/{id}` | empty | admin, manager, operator |
| GET | `/api/groups` | `List<ClientGroupDto>` | authenticated |
| POST | `/api/groups` | `ClientGroupDto` | admin, manager, operator |
| PUT | `/api/groups/{id}` | `ClientGroupDto` | admin, manager, operator |
| DELETE | `/api/groups/{id}` | empty | admin, manager, operator |
| GET | `/api/templates` | `List<EmailTemplateDto>` | authenticated |
| GET | `/api/templates/pending` | `List<EmailTemplateDto>` | authenticated |
| POST | `/api/templates` | `EmailTemplateDto` | admin, manager, operator |
| PUT | `/api/templates/{id}` | `EmailTemplateDto` | admin, manager, operator |
| DELETE | `/api/templates/{id}` | empty | admin, manager, operator |
| POST | `/api/templates/{id}/submit` | `EmailTemplateDto` | admin, manager, operator |
| POST | `/api/templates/{id}/approve` | `EmailTemplateDto` | admin, manager |
| POST | `/api/templates/{id}/reject` | `EmailTemplateDto` | admin, manager |
| GET | `/api/accounts/smtp` | `List<SmtpAccountDto>` | authenticated |
| POST | `/api/accounts/smtp` | `SmtpAccountDto` | admin, operator |
| PUT | `/api/accounts/smtp/{id}` | `SmtpAccountDto` | admin, operator |
| DELETE | `/api/accounts/smtp/{id}` | empty | admin, operator |
| GET | `/api/accounts/gmail` | `List<GmailAccountDto>` | authenticated |
| POST | `/api/accounts/gmail` | `GmailAccountDto` | admin, operator |
| PUT | `/api/accounts/gmail/{id}` | `GmailAccountDto` | admin, operator |
| DELETE | `/api/accounts/gmail/{id}` | empty | admin, operator |
| POST | `/api/campaigns` | `CampaignDto` | admin, manager, operator |
| GET | `/api/campaigns` | `PagedResponse<CampaignDto>` | authenticated |
| GET | `/api/campaigns/{id}` | `CampaignDto` | authenticated |
| GET | `/api/campaigns/{id}/deliveries` | `List<DeliveryDto>` | authenticated |
| POST | `/api/campaigns/{id}/cancel` | `CampaignDto` | admin, manager, operator |
| GET | `/api/outbox` | `List<OutboxDto>` | authenticated |
| POST | `/api/outbox` | `OutboxDto` | admin, manager, operator |
| POST | `/api/outbox/{id}/cancel` | `OutboxDto` | admin, manager, operator |
| GET | `/api/extraction` | `List<ExtractionRunDto>` | authenticated |
| POST | `/api/extraction` | `ExtractionRunDto` | admin, manager, operator |
| GET | `/api/extraction/{id}` | `ExtractionRunDto` | authenticated |
| GET | `/api/extraction/{id}/results` | `PagedResponse<ExtractionResultDto>` | authenticated |
| GET | `/api/inbox` | `List<InboxMessageDto>` | authenticated |
| GET | `/api/dashboard` | `DashboardDto` | authenticated |
| GET | `/api/me` | `UserDto` | authenticated |
| POST | `/api/me/password` | empty | authenticated |
| GET | `/api/users` | `List<UserDto>` | admin |

## Work Guidance

- New endpoint checklist: tenant filter, correct HTTP verb, CSRF-safe, envelope response,
  no secret in the body, and a test.
- Keep controllers thin. If a controller grows a conditional or a loop over business data,
  the logic belongs in a service.
- A service that reads a row as part of a list and never on its own is read through that
  list, so the row is picked out of the list of the tenant.

## Verification

- `./mvnw test` - MockMvc tests cover the endpoint contract, the status codes, the envelope
  shape and the tenant filter.

## Child DOX Index

This package has no child DOX files.
