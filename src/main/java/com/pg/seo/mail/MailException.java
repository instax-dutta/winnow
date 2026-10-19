package com.pg.seo.mail;

/**
 * A provider refused or failed to deliver a message.
 *
 * <p>The message of this exception is always redacted. It never carries a credential, a
 * resolved secret or the verbatim response of a mail server, because those are written to
 * logs by whoever handles the exception.
 */
public class MailException extends RuntimeException {

    public MailException(String message) {
        super(message);
    }

    public MailException(String message, Throwable cause) {
        super(message, cause);
    }
}
