package io.github.instaxdutta.winnow.domain;

/** Lifecycle of a send campaign. */
public enum CampaignStatus {
    QUEUED,
    RUNNING,
    SUCCESS,
    PARTIAL,
    FAILED,
    CANCELLED
}
