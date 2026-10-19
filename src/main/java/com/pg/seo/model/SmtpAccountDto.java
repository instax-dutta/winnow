package com.pg.seo.model;

import com.pg.seo.domain.AccountProtocol;
import com.pg.seo.domain.AuthMethod;

/**
 * An SMTP account as it leaves the service layer.
 *
 * <p>There is no password field. {@code hasCredential} says whether a secret is stored, and
 * nothing else about the secret is exposed. The original returned the plaintext SMTP
 * password to any logged-in user.
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
