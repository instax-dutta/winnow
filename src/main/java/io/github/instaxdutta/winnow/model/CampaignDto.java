package io.github.instaxdutta.winnow.model;

import io.github.instaxdutta.winnow.domain.AccountKind;
import io.github.instaxdutta.winnow.domain.CampaignStatus;
import io.github.instaxdutta.winnow.domain.TargetType;
import java.time.Instant;

public record CampaignDto(
        Long id,
        AccountKind fromAccountKind,
        Long fromAccountId,
        String fromEmail,
        TargetType targetType,
        Long templateId,
        String templateName,
        int recipientCount,
        int successCount,
        int failureCount,
        CampaignStatus status,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt) {}
