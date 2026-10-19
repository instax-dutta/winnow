package com.pg.seo.model;

import com.pg.seo.domain.DeliveryStatus;
import java.time.Instant;

public record DeliveryDto(
        Long id,
        String recipientEmail,
        DeliveryStatus status,
        String providerMessageId,
        String errorDetail,
        Instant sentAt) {}
