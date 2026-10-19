package com.pg.seo.domain;

/** Lifecycle of an email template. */
public enum TemplateStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    ARCHIVED
}
