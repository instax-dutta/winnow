package io.github.instaxdutta.winnow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** One send campaign: who sent, to whom, using which template, and how it went. */
@Getter
@Setter
@Entity
@Table(name = "campaign")
public class Campaign extends TenantOwnedEntity {

    @Column(name = "from_account_id")
    private Long fromAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_account_kind", nullable = false, length = 16)
    private AccountKind fromAccountKind = AccountKind.SMTP;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 16)
    private TargetType targetType = TargetType.GROUP;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private EmailTemplate template;

    @Column(name = "recipient_count", nullable = false)
    private int recipientCount;

    @Column(name = "success_count", nullable = false)
    private int successCount;

    @Column(name = "failure_count", nullable = false)
    private int failureCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CampaignStatus status = CampaignStatus.QUEUED;

    @Column(name = "error_detail", columnDefinition = "TEXT")
    private String errorDetail;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;
}
