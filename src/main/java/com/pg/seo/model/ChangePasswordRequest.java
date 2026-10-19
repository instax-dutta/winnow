package com.pg.seo.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank @Size(min = 12, max = 200) String currentPassword,
        @NotBlank @Size(min = 12, max = 200) String newPassword) {}
