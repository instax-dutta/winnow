package com.pg.seo.domain;

/** Lifecycle of a send campaign. */
public enum CampaignStatus {
    QUEUED,
    RUNNING,
    SUCCESS,
    PARTIAL,
    FAILED,
    CANCELLED
}
