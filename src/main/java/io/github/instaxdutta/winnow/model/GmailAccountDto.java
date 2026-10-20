package io.github.instaxdutta.winnow.model;

/**
 * A Google OAuth account as it leaves the service layer.
 *
 * <p>There is no client secret and no refresh token field. Returning all four OAuth fields
 * - including a live refresh token - to any logged-in user hands over the account.
 */
public record GmailAccountDto(
        Long id,
        String email,
        String oauthClientId,
        String scopes,
        boolean enabled,
        boolean hasCredential) {}
