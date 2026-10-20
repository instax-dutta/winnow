package io.github.instaxdutta.winnow.model;

import java.time.Instant;

public record InboxMessageDto(
        Long id,
        String fromAddress,
        String subject,
        String bodyHtml,
        Instant receivedAt,
        boolean read) {}
