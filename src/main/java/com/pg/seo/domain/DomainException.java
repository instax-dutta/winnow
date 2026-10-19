package com.pg.seo.domain;

/** Base for the exceptions the service layer raises. */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
