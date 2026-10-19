package com.pg.seo.domain;

/** Lifecycle of an extraction run. */
public enum ExtractionStatus {
    QUEUED,
    RUNNING,
    SUCCESS,
    PARTIAL,
    FAILED
}
