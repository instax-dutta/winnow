package com.pg.seo.model;

public record ClientDto(
        Long id,
        String name,
        String email,
        String webLink,
        Long groupId,
        String groupCode) {}
