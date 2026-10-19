package com.pg.seo.model;

import com.pg.seo.domain.AccountKind;
import com.pg.seo.domain.CampaignStatus;
import com.pg.seo.domain.TargetType;
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
