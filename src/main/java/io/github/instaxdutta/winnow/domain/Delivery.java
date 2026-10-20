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

/** The delivery outcome for one recipient inside a campaign. */
@Getter
@Setter
@Entity
@Table(name = "delivery")
public class Delivery extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(name = "recipient_email", nullable = false, length = 254)
    private String recipientEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private EmailTemplate template;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private DeliveryStatus status = DeliveryStatus.QUEUED;

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Column(name = "error_detail", columnDefinition = "TEXT")
    private String errorDetail;

    @Column(name = "sent_at")
    private Instant sentAt;
}
