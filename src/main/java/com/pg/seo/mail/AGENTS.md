# mail/

## Purpose

Outbound mail. Provider adapters that turn a send request into a message, plus the template
rendering and the quota accounting.

## Ownership

- `MailSender` - the interface every provider implements.
- `SendRequest` - one message for one recipient.
- `SendResult` - the outcome of one send.
- `MailException` - a provider refused or failed to deliver.
- `SmtpMailSender` - sends through an SMTP account.
- `GmailMailSender` - sends through a Google OAuth account.
- `MimeMessageBuilder` - builds the MIME body shared by both providers.
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
- A failure message never carries a credential and never carries the verbatim response of a
  mail server. Only the type of the failure is reported.
- `SendRequest` names the sending account by `ownerUserId` and `accountId`, because an
  adapter reads the account row to find the credential and every read of tenant data
  filters on the owner id.

## Work Guidance

- Adding a provider: implement `MailSender`, register it, and add the account fields it
  needs to the account entity and the migration.
- A credential lookup happens inside the adapter, not in the service, so the service never
  holds a secret.
- Test a provider against a local fake server or a mocked transport, never a live mailbox.

## Public API

```java
public interface MailSender {
    SendResult send(SendRequest request) throws MailException;
    String providerName();
}

public record SendRequest(
        String fromEmail, String toEmail, String subject, String htmlBody,
        String textBody, Map<String,String> variables, String replyTo,
        Long ownerUserId, Long accountId) { /* a seven argument form without the
                                                 account scope also exists */ }

public record SendResult(
        boolean success, String providerMessageId, String errorMessage, String failureCode) { }

public class MailException extends RuntimeException { }
```

- `SmtpMailSender` and `GmailMailSender` take the account repository, the `SecretStore` and
  the `MailProperties`. Both resolve their credential at call time. They are two beans of
  the `MailSender` type, so a caller injects the concrete type or qualifies the bean
  (`smtpMailSender`, `gmailMailSender`).
- `MailTemplateRenderer.render(String contentHtml, Map<String,String> variables)` replaces
  every `{{Name}}` placeholder. Every substituted value is HTML escaped. There is no raw
  mode. A dotted name is looked up as the whole key first and then as a path into a nested
  map.
- `QuotaService.countByStatus`, `sentToday`, `sentThisHour`, `sentThisMonth` and
  `assertWithinQuota` count the deliveries of a tenant and a sending account. The windows
  are calendar windows in UTC.

## Verification

- `./mvnw test` - the mail tests assert message structure, per-recipient failure isolation,
  timeout configuration, and that no credential appears in any log or serialized response.

## Child DOX Index

This package has no child DOX files.
