package io.github.instaxdutta.winnow.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Sending configuration.
 *
 * <p>Every timeout is explicit. Without one, a single unreachable mail server blocks a
 * thread forever.
 */
@Validated
@ConfigurationProperties(prefix = "seo.mail")
public record MailProperties(
        Duration connectTimeout,
        Duration readTimeout,
        Duration writeTimeout,
        int perAccountDailyLimit,
        int perAccountHourlyLimit,
        int batchSize,
        Duration retryBackoff) {

    public MailProperties {
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(15) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(30) : readTimeout;
        writeTimeout = writeTimeout == null ? Duration.ofSeconds(30) : writeTimeout;
        perAccountDailyLimit = perAccountDailyLimit <= 0 ? 500 : perAccountDailyLimit;
        perAccountHourlyLimit = perAccountHourlyLimit <= 0 ? 50 : perAccountHourlyLimit;
        batchSize = batchSize <= 0 ? 200 : batchSize;
        retryBackoff = retryBackoff == null ? Duration.ofSeconds(5) : retryBackoff;
    }
}
