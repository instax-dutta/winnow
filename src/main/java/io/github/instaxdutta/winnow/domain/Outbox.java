package io.github.instaxdutta.winnow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * A scheduled message waiting to be dispatched.
 *
 * <p>The dispatch job claims a row with a conditional status update, so a row is never
 * sent twice even if two instances of the application run at the same time.
 */
@Getter
@Setter
@Entity
@Table(name = "outbox")
public class Outbox extends TenantOwnedEntity {

    @Column(name = "from_account_id")
    private Long fromAccountId;

    @Column(name = "to_email", nullable = false, length = 254)
    private String toEmail;

    @Column(name = "draft_id", length = 255)
    private String draftId;

    @Column(name = "subject", length = 998)
    private String subject;

    @Column(name = "body_html", columnDefinition = "MEDIUMTEXT")
    private String bodyHtml;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OutboxStatus status = OutboxStatus.SCHEDULED;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
