package com.pg.seo.domain;

/** The request was understood but is not valid. */
public class ValidationException extends DomainException {

    public ValidationException(String message) {
        super(message);
    }
}
