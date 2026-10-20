package io.github.instaxdutta.winnow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Sending quota configuration.
 *
 * <p>The limits are separate for the hour, the day and the month, and they are applied
 * per sending account. A single cap of 50,000 that is labelled as a daily cap but is
 * really monthly, with no per-account pacing at all, lets one account send far more than
 * it is allowed to.
 *
 * <p>Created by the service layer leaf because the configuration leaf had not written it
 * yet. It carries the exact shape the service layer codes against.
 */
@Validated
@ConfigurationProperties(prefix = "seo.quota")
public record QuotaProperties(int dailySendLimit, int hourlySendLimit, int monthlySendLimit) {

    public QuotaProperties {
        dailySendLimit = dailySendLimit <= 0 ? 500 : dailySendLimit;
        hourlySendLimit = hourlySendLimit <= 0 ? 50 : hourlySendLimit;
        monthlySendLimit = monthlySendLimit <= 0 ? 50000 : monthlySendLimit;
    }
}
