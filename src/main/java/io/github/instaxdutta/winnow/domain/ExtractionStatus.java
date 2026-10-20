package io.github.instaxdutta.winnow.domain;

/** Lifecycle of an extraction run. */
public enum ExtractionStatus {
    QUEUED,
    RUNNING,
    SUCCESS,
    PARTIAL,
    FAILED
}
