# security/

## Purpose

Authentication, authorization, the principal model and the HTTP security policy. This package
is the single place that answers "who is calling and are they allowed to".

## Ownership

- `SecurityConfig` - the filter chain.
- `AuthPrincipal` - the authenticated caller: `userId`, `username`, authorities.
- `AuthPrincipalResolver` - reads the caller out of the security context for the web layer.
- `AppUserDetailsService` - loads a user and its authorities, and keeps the login counter.
- `AppUserPrincipal` - the user details that carry the tenant key.
- `AuthenticationEventRecorder` - counts a failed login and stamps a successful one.
- `RoleNames` - the role codes and the `@PreAuthorize` vocabulary.
- `PasswordConfig` - the password encoder.
- `LoginFailureHandler` - the one answer to every rejected login.
- `AuthPrincipalAuthenticationSuccessHandler` - puts the principal on the session.

## Local Contracts

- Form login with a BCrypt-encoded password. Cost 12.
- Sessions: `IF_REQUIRED`, `maximumSessions(1)`, with a `SessionRegistry` and an
  `HttpSessionEventPublisher` so expired sessions are actually evicted. The original omitted
  both and leaked sessions.
- Session fixation protection stays at the Spring default (`changeSessionId` on login).
- CSRF stays enabled. The front end reads the token from the `_csrf` meta tag. A mutating
  request with no token is answered with a redirect to the login page, because Spring
  Security reads a missing token as a lost session; a request with a wrong token, or a
  request that holds no role, is answered with 403.
- Authorization is declared in the filter chain by path, and additionally by a
  `@PreAuthorize` on the service method for anything that changes data. The expressions
  are the constants in `RoleNames`, so the chain and the services speak one vocabulary.
- Roles are `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_OPERATOR`, `ROLE_VIEWER`. The original had
  `ROLE_ADMIN/ROLE_EMP/ROLE_MANG/ROLE_HR` and a `hasRole("USER")` match that no seeded role
  could satisfy, so `GET /` returned 403 for everyone.
- The login response never reveals whether the failure was a bad password, an unknown user, a
  disabled account, or a failed lockout. One message for all of them.
- Failed logins are counted and the account locks for `seo.security.lock-duration` once the
  count reaches `seo.security.max-failed-logins`. The original had no lockout at all.
- The user id is read from the user details the authentication carries, never from a request
  parameter.
- **No licence gating.** This project is open source. There is no licence token, no licence
  key, no activation check, and no machine fingerprinting. The original shipped an HS256
  licence token whose signature was never verified, so it was forgeable, and it expired on
  2025-09-18, which locked the application out entirely. Neither the token nor the
  mechanism is carried over, and `tools/verify/code-hygiene.mjs` fails the build if a
  licence check or a `getHardwareAddress` call reappears. Authentication and roles are not
  licensing and stay.

## Work Guidance

- Add a role: it needs a row in the `app_role` table, a constant in `RoleNames`, and a
  filter-chain rule.
- Never write an authorization decision in a controller. If a check is needed in a service,
  use `@PreAuthorize`.
- Never log a submitted password, a token, a password hash or a session id.

## Verification

- `./mvnw test` - the security tests cover login success and failure, lockout, CSRF on
  mutating endpoints, session eviction, and role-based denial.

## Child DOX Index

This package has no child DOX files.
