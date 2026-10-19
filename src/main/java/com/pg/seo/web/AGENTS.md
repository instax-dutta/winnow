# web/

## Purpose

The HTTP layer. It translates HTTP into a service call and a service result back into HTTP.
It holds no business rules and touches no repository directly.

## Ownership

- `@RestController` classes - one per resource area.
- `ApiResponse<T>` - the response envelope every JSON endpoint returns.
- `GlobalExceptionHandler` - maps domain exceptions to status codes.
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
- Paginated endpoints return `PageResponse<T>` with a real total count.

## Work Guidance

- New endpoint checklist: tenant filter, correct HTTP verb, CSRF-safe, envelope response,
  no secret in the body, and a test.
- Keep controllers thin. If a controller grows a conditional or a loop over business data, the
  logic belongs in a service.

## Verification

- `./mvnw test` - MockMvc tests cover the endpoint contract, the status codes, the envelope
  shape and the tenant filter.

## Child DOX Index

This package has no child DOX files.
