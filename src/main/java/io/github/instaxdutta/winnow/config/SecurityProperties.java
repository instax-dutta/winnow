package io.github.instaxdutta.winnow.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Login protection.
 *
 * <p>A run of failed logins locks the account for a bounded window. Without a lockout a
 * stolen password can be tried forever.
 */
@Validated
@ConfigurationProperties(prefix = "seo.security")
public record SecurityProperties(int maxFailedLogins, Duration lockDuration) {

    public SecurityProperties {
        maxFailedLogins = maxFailedLogins <= 0 ? 5 : maxFailedLogins;
        lockDuration = lockDuration == null || lockDuration.isNegative() || lockDuration.isZero()
                ? Duration.ofMinutes(15)
                : lockDuration;
    }
}
