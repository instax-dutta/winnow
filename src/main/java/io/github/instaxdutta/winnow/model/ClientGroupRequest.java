package io.github.instaxdutta.winnow.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientGroupRequest(
        @NotBlank @Size(max = 32) String code,
        @NotBlank @Size(max = 255) String name) {}
