package com.pg.seo.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailTemplateRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 998) String subject,
        @NotBlank String contentHtml) {}
