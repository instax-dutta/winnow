package com.pg.seo.domain;

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

/** An email template: a subject and an HTML body, with an approval workflow. */
@Getter
@Setter
@Entity
@Table(name = "email_template")
public class EmailTemplate extends TenantOwnedEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "subject", nullable = false, length = 998)
    private String subject;

    @Column(name = "content_html", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String contentHtml;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TemplateStatus status = TemplateStatus.DRAFT;

    @Column(name = "version", nullable = false)
    private int version = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private AppUser approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;
}
