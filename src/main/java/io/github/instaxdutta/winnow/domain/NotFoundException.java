package io.github.instaxdutta.winnow.domain;

/** The requested resource does not exist, or does not belong to the calling tenant. */
public class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException of(String what, Object id) {
        return new NotFoundException(what + " " + id + " was not found");
    }
}
