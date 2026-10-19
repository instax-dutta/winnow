package com.pg.seo.config;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Where uploaded and generated files are written.
 */
@Validated
@ConfigurationProperties(prefix = "seo.storage")
public record StorageProperties(Path uploadDir) {

    public StorageProperties {
        uploadDir = uploadDir == null ? Path.of("./data/exports") : uploadDir;
    }
}
