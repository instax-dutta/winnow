package com.pg.seo.model;

import com.pg.seo.domain.AccountProtocol;
import com.pg.seo.domain.AuthMethod;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SmtpAccountRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull AccountProtocol protocol,
        @NotBlank @Size(max = 255) String host,
        @Min(1) @Max(65535) int port,
        @NotBlank @Size(max = 254) String username,
        @NotNull AuthMethod authMethod,
        boolean enabled,
        /* The secret is write-only: accepted on input, never returned. */
        @Size(max = 512) String secret) {}
