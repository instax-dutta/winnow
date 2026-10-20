package io.github.instaxdutta.winnow.model;

import io.github.instaxdutta.winnow.domain.AccountKind;

/** A sending account, identified only by its kind and id. */
public record AccountKindDto(AccountKind kind, Long id, String email) {}
