package com.pg.seo.model;

import com.pg.seo.domain.ExtractionStatus;
import java.time.Instant;

public record ExtractionRunDto(
        Long id,
        ExtractionStatus status,
        long domainCount,
        String sourceFileName,
        Instant startedAt,
        Instant finishedAt,
        String errorDetail) {}
