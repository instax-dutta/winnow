package com.pg.seo.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record OutboxRequest(
        @NotNull Long fromAccountId,
        @NotBlank @Email @Size(max = 254) String toEmail,
        @Size(max = 998) String subject,
        String bodyHtml,
        @NotNull Instant scheduledAt) {}
