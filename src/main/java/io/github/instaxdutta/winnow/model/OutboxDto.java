package io.github.instaxdutta.winnow.model;

import io.github.instaxdutta.winnow.domain.OutboxStatus;
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
