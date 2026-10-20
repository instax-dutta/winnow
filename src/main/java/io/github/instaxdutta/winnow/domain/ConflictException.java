package io.github.instaxdutta.winnow.domain;

/** The request conflicts with the current state, for example a duplicate unique key. */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }
}
