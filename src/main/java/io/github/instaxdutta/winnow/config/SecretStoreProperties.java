package io.github.instaxdutta.winnow.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Where the file backed secret store keeps its secrets.
 *
 * <p>The path is a deployment concern, so it is bound from the environment rather than
 * hardcoded. Nothing in this record is a secret.
 */
@Validated
@ConfigurationProperties(prefix = "seo.secret-store")
public record SecretStoreProperties(Path path) {

    public SecretStoreProperties {
        path = path == null ? Path.of("./.secrets") : path;
    }
}
