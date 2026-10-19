package com.pg.seo.mail;

/**
 * A provider that can deliver a message.
 *
 * <p>An adapter resolves the credential it needs from the secret store at call time and
 * holds it only for the duration of the call. It never caches a secret, never logs one and
 * never returns one.
 */
public interface MailSender {

    /**
     * Delivers one message to one recipient.
     *
     * @throws MailException when the provider refused or failed to deliver the message
     */
    SendResult send(SendRequest request) throws MailException;

    /**
     * The stable name of the provider, for example {@code smtp} or {@code gmail}.
     */
    String providerName();
}
