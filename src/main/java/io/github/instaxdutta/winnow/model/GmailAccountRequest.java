package io.github.instaxdutta.winnow.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GmailAccountRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 255) String oauthClientId,
        @Size(max = 512) String scopes,
        boolean enabled,
        /* Write-only. Accepted on input, stored in the secret store, never returned. */
        @Size(max = 512) String clientSecret,
        @Size(max = 2048) String refreshToken) {}
