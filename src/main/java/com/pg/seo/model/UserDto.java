package com.pg.seo.model;

import com.pg.seo.domain.UserStatus;
import java.time.Instant;
import java.util.Set;

/** A user. There is no password hash field and there never will be. */
public record UserDto(
        Long id,
        String username,
        String email,
        String employeeId,
        UserStatus status,
        Set<String> roles,
        Instant lastLoginAt) {}
