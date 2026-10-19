package com.pg.seo.model;

import com.pg.seo.domain.TemplateStatus;
import java.time.Instant;

public record EmailTemplateDto(
        Long id,
        String name,
        String subject,
        String contentHtml,
        TemplateStatus status,
        int version,
        String approvedBy,
        Instant approvedAt,
        Instant updatedAt) {}
