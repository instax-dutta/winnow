package io.github.instaxdutta.winnow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** A captured inbound message. */
@Getter
@Setter
@Entity
@Table(name = "inbox_message")
public class InboxMessage extends AuditedEntity {

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "provider_message_id", nullable = false, length = 255)
    private String providerMessageId;

    @Column(name = "from_address", nullable = false, length = 254)
    private String fromAddress;

    @Column(name = "subject", length = 998)
    private String subject;

    @Column(name = "body_html", columnDefinition = "MEDIUMTEXT")
    private String bodyHtml;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
