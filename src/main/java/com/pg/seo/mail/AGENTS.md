# mail/

## Purpose

Outbound mail. Provider adapters that turn a send request into a message, plus the template
rendering and the quota accounting.

## Ownership

- `MailSender` - the interface every provider implements.
- `SmtpMailSender` - sends through an SMTP account.
- `GmailMailSender` - sends through a Google OAuth account.
- `MailTemplateRenderer` - renders the HTML body from a template and a recipient context.
- `QuotaService` - counts what was sent and what remains.

## Local Contracts

- A provider adapter receives a credential from the secret store at call time and holds it only
  for the duration of the call. It is never cached on a field, never written to a log, never
  returned to the client.
- Every SMTP connection has explicit connect, read and write timeouts. The original left
  them at the infinite default, so one stalled server blocked a thread forever.
- One `MimeMessage` per recipient. The original reused a single mutable message across
  recipients, which leaked partially-applied state on failure.
- Every message carries a `Message-ID`, a plain-text alternative, and a correct `From`.
- The subject is a separate field, not the template name. The original used the template name
  as the subject.
- The HTML body is rendered from a template with the recipient's fields escaped. The original
  used `th:utext`, so any operator-authored HTML was injected raw.
- A send is idempotent per queue row: a row is claimed with a status transition before the
  send and is never sent twice.
- Rate limiting is per sending account and configurable. The original had a monthly cap of
  50,000 mislabelled as a daily cap, and no per-account pacing at all.
- A per-recipient failure never fails the batch. The failure and its cause are recorded.

## Work Guidance

- Adding a provider: implement `MailSender`, register it, and add the account fields it
  needs to the account entity and the migration.
- A credential lookup happens inside the adapter, not in the service, so the service never
  holds a secret.
- Test a provider against a local fake server or a mocked transport, never a live mailbox.

## Verification

- `./mvnw test` - the mail tests assert message structure, per-recipient failure isolation,
  timeout configuration, and that no credential appears in any log or serialized response.

## Child DOX Index

This package has no child DOX files.
