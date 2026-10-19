# model/

## Purpose

The request and response shapes. This package is the shared vocabulary between the service
layer and the web layer: a service returns a DTO, the web layer puts that DTO in the
response envelope.

## Ownership

- One record per request or response shape.
- `ApiResponse` lives in `web/`, not here, because it is an HTTP concern.

## Local Contracts

- Every DTO is a `record`. No setters, no mutable state.
- **A DTO never carries a credential.** There is no password field, no client secret field,
  no refresh token field and no token field. Where the caller needs to know whether a
  credential is configured, the DTO carries a boolean such as `hasCredential` and nothing
  more.
- A DTO never exposes a JPA entity. If a shape is a subset of an entity, it is written out
  field by field.
- A DTO field is a primitive or a String, not another DTO, unless the nesting is one level
  deep and meaningful.
- Validation annotations on a request record are the single source of truth for what a
  request must contain. The web layer does not re-check them.
- A request that carries a write-only value (a password, a token) names that value so it is
  obvious it is never returned. The service layer stores it and drops it.

## Work Guidance

- Adding a field to a response changes the API contract. Update the templates that read it
  in the same change.
- A field that is derived is computed in the service, never in the web layer.
- Keep the names aligned with the domain: `ownerUserId`, `groupId`, `templateId`.

## Verification

- `./mvnw test` - the MockMvc tests assert the exact JSON shape of the responses.

## Child DOX Index

This package has no child DOX files.
