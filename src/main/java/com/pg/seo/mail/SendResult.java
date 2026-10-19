package com.pg.seo.mail;

/**
 * The outcome of one send.
 *
 * <p>{@code errorMessage} is redacted. It never carries a credential or the verbatim
 * response of a mail server.
 */
public record SendResult(
        boolean success,
        String providerMessageId,
        String errorMessage,
        String failureCode) {

    public static SendResult success(String providerMessageId) {
        return new SendResult(true, providerMessageId, null, null);
    }

    public static SendResult failure(String failureCode, String errorMessage) {
        return new SendResult(false, null, errorMessage, failureCode);
    }
}
