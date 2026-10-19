package com.pg.seo.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The scraper sidecar.
 *
 * <p>The sidecar is reached over loopback by default.
 *
 * <p>{@code allowedHostSuffixes} is an extra, operator-controlled restriction on top of the
 * private-range block, which is unconditional and cannot be switched off. When the suffix
 * list is empty and {@code allowArbitraryHosts} is true, any public host may be crawled.
 * Set {@code allowArbitraryHosts} to false to restrict crawling to the suffix list.
 */
@Validated
@ConfigurationProperties(prefix = "seo.scraper")
public record ScraperProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout,
        boolean allowArbitraryHosts,
        List<String> allowedHostSuffixes) {

    public ScraperProperties {
        baseUrl = baseUrl == null || baseUrl.isBlank() ? "http://127.0.0.1:3000" : baseUrl;
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(5) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(15) : readTimeout;
        allowedHostSuffixes = allowedHostSuffixes == null ? List.of() : List.copyOf(allowedHostSuffixes);
    }
}
