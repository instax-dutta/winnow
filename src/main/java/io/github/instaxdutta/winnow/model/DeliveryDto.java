package io.github.instaxdutta.winnow.model;

import io.github.instaxdutta.winnow.domain.DeliveryStatus;
import java.time.Instant;

public record DeliveryDto(
        Long id,
        String recipientEmail,
        DeliveryStatus status,
        String providerMessageId,
        String errorDetail,
        Instant sentAt) {}
