package io.github.instaxdutta.winnow.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Extraction configuration.
 *
 * <p>The worker count and the queue depth are bounded on purpose. One unbounded task per
 * spreadsheet row exhausts memory on a large file.
 */
@Validated
@ConfigurationProperties(prefix = "seo.extraction")
public record ExtractionProperties(
        Duration connectTimeout,
        Duration readTimeout,
        int maxResponseBytes,
        int maxRedirects,
        int workerThreads,
        int queueCapacity,
        int perDomainTimeoutSeconds,
        int maxDomainsPerRun,
        int writeBatchSize) {

    public ExtractionProperties {
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(5) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(10) : readTimeout;
        maxResponseBytes = maxResponseBytes <= 0 ? 2 * 1024 * 1024 : maxResponseBytes;
        maxRedirects = maxRedirects < 0 ? 0 : maxRedirects;
        workerThreads = workerThreads <= 0 ? 8 : workerThreads;
        queueCapacity = queueCapacity <= 0 ? 500 : queueCapacity;
        perDomainTimeoutSeconds = perDomainTimeoutSeconds <= 0 ? 15 : perDomainTimeoutSeconds;
        maxDomainsPerRun = maxDomainsPerRun <= 0 ? 5000 : maxDomainsPerRun;
        writeBatchSize = writeBatchSize <= 0 ? 200 : writeBatchSize;
    }
}
