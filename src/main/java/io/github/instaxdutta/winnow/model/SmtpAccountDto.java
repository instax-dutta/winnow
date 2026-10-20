package io.github.instaxdutta.winnow.model;

import io.github.instaxdutta.winnow.domain.AccountProtocol;
import io.github.instaxdutta.winnow.domain.AuthMethod;

/**
 * An SMTP account as it leaves the service layer.
 *
 * <p>There is no password field. {@code hasCredential} says whether a secret is stored, and
 * nothing else about the secret is exposed. Returning the plaintext SMTP password to any
 * logged-in user hands over the account.
 */
public record SmtpAccountDto(
        Long id,
        String email,
        AccountProtocol protocol,
        String host,
        int port,
        String username,
        AuthMethod authMethod,
        boolean enabled,
        boolean hasCredential) {}
