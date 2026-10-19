package com.pg.seo.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Login protection.
 *
 * <p>The original application had no lockout at all, so a stolen password could be tried
 * forever. Here a run of failed logins locks the account for a bounded window.
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
