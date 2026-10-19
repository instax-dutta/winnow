package com.pg.seo.domain;

/** Lifecycle of a scheduled outbox row. */
public enum OutboxStatus {
    SCHEDULED,
    SENDING,
    SENT,
    FAILED,
    CANCELLED
}
