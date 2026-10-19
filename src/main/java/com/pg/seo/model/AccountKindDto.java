package com.pg.seo.model;

import com.pg.seo.domain.AccountKind;

/** A sending account, identified only by its kind and id. */
public record AccountKindDto(AccountKind kind, Long id, String email) {}
