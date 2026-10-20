package io.github.instaxdutta.winnow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The first administrator of a fresh install.
 *
 * <p>When the password is blank nothing happens. When it is set, the named user gets that
 * password and is activated. This is a deployment convenience, not an activation mechanism:
 * the application carries no licence, no token and no machine check.
 */
@Validated
@ConfigurationProperties(prefix = "seo.bootstrap")
public record BootstrapProperties(String username, String password) {

    public BootstrapProperties {
        username = username == null || username.isBlank() ? "admin" : username;
        password = password == null ? "" : password;
    }
}
