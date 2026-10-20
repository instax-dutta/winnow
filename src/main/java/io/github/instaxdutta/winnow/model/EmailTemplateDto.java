package io.github.instaxdutta.winnow.model;

import io.github.instaxdutta.winnow.domain.TemplateStatus;
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
