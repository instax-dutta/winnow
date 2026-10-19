package com.pg.seo.domain;

/** Outcome for one recipient inside a campaign. */
public enum DeliveryStatus {
    QUEUED,
    SENT,
    FAILED,
    BOUNCED
}
