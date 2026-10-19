package com.pg.seo.domain;

/** The caller is authenticated but is not allowed to perform this action. */
public class ForbiddenException extends DomainException {

    public ForbiddenException(String message) {
        super(message);
    }
}
