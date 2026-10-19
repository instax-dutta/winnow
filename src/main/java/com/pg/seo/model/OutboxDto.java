package com.pg.seo.model;

import com.pg.seo.domain.OutboxStatus;
import java.time.Instant;

public record OutboxDto(
        Long id,
        Long fromAccountId,
        String toEmail,
        String subject,
        Instant scheduledAt,
        OutboxStatus status,
        int attemptCount,
        String errorMessage) {}
