package io.github.instaxdutta.winnow.domain;

/** Outcome for one recipient inside a campaign. */
public enum DeliveryStatus {
    QUEUED,
    SENT,
    FAILED,
    BOUNCED
}
