package io.github.instaxdutta.winnow.domain;

/** Lifecycle of a scheduled outbox row. */
public enum OutboxStatus {
    SCHEDULED,
    SENDING,
    SENT,
    FAILED,
    CANCELLED
}
