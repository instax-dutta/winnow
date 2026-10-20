package io.github.instaxdutta.winnow.mail;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One message, for one recipient.
 *
 * <p>The first seven components are the message itself. The last two identify the sending
 * account inside its tenant: a provider adapter has to read the account row to find the
 * credential, and every read of tenant data filters on the owner id.
 *
 * <p>{@code variables} are the template variables for this recipient. They are substituted
 * into the HTML body by the renderer, and every substituted value is HTML escaped.
 */
public record SendRequest(
        String fromEmail,
        String toEmail,
        String subject,
        String htmlBody,
        String textBody,
        Map<String, String> variables,
        String replyTo,
        Long ownerUserId,
        Long accountId) {

    public SendRequest {
        variables = variables == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(variables));
    }

    /**
     * A request that does not name a tenant account. A provider adapter cannot resolve the
     * account without a tenant, so it rejects this form.
     */
    public SendRequest(
            String fromEmail,
            String toEmail,
            String subject,
            String htmlBody,
            String textBody,
            Map<String, String> variables,
            String replyTo) {
        this(fromEmail, toEmail, subject, htmlBody, textBody, variables, replyTo, null, null);
    }
}
