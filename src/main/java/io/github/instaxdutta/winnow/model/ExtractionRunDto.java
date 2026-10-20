package io.github.instaxdutta.winnow.model;

import io.github.instaxdutta.winnow.domain.ExtractionStatus;
import java.time.Instant;

public record ExtractionRunDto(
        Long id,
        ExtractionStatus status,
        long domainCount,
        String sourceFileName,
        Instant startedAt,
        Instant finishedAt,
        String errorDetail) {}
