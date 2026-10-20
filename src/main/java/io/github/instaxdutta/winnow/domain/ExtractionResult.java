package io.github.instaxdutta.winnow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * One extracted contact address.
 *
 * <p>{@code emailHash} is the SHA-256 of the normalised address. The unique index on
 * (run_id, email_hash) is the deduplication constraint; without it, re-running the same
 * domain duplicates every row.
 */
@Getter
@Setter
@Entity
@Table(name = "extraction_result")
public class ExtractionResult extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private ExtractionRun run;

    @Column(name = "domain", nullable = false, length = 255)
    private String domain;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "email_hash", nullable = false)
    private byte[] emailHash;

    @Column(name = "source_row")
    private Integer sourceRow;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
