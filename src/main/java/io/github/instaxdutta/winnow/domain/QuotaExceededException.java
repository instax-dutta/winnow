package io.github.instaxdutta.winnow.domain;

/** The caller has used up a configured quota, for example the daily send allowance. */
public class QuotaExceededException extends DomainException {

    public QuotaExceededException(String message) {
        super(message);
    }
}
