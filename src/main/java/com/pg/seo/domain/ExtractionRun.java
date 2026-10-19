package com.pg.seo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** One run of the domain-to-email extraction over an uploaded domain list. */
@Getter
@Setter
@Entity
@Table(name = "extraction_run")
public class ExtractionRun extends TenantOwnedEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ExtractionStatus status = ExtractionStatus.QUEUED;

    @Column(name = "domain_count", nullable = false)
    private long domainCount;

    @Column(name = "source_file_name", length = 512)
    private String sourceFileName;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "error_detail", columnDefinition = "TEXT")
    private String errorDetail;
}
