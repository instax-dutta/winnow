package io.github.instaxdutta.winnow.extraction;

import io.github.instaxdutta.winnow.config.ExtractionProperties;
import io.github.instaxdutta.winnow.domain.ExtractionStatus;
import io.github.instaxdutta.winnow.domain.LogSafe;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Runs the crawl of a run of domains.
 *
 * <p>The fan-out is bounded twice over. The work is submitted in chunks of
 * {@code workerThreads} tasks and the next chunk is only submitted once the current one has
 * finished, so at most that many crawls are in flight. The executor itself has a bounded
 * queue, so a queue that is full never grows.
 *
 * <p>The deduplication set belongs to the run and dies with it. A map kept for the whole
 * process and never cleared makes the second run of the same domain find nothing.
 *
 * <p>The run always ends in {@code SUCCESS}, {@code PARTIAL} or {@code FAILED}. It is never
 * left in {@code RUNNING}, whatever happens.
 */
@Component
public class ExtractionWorker {

    private static final Logger LOG = LoggerFactory.getLogger(ExtractionWorker.class);

    private final DomainCrawler crawler;
    private final ExtractionProperties properties;
    private final ObjectProvider<Executor> executorProvider;

    private volatile Executor fallbackExecutor;

    public ExtractionWorker(
            DomainCrawler crawler,
            ExtractionProperties properties,
            @Qualifier("extractionExecutor") ObjectProvider<Executor> executorProvider) {
        this.crawler = crawler;
        this.properties = properties;
        this.executorProvider = executorProvider;
    }

    /**
     * Crawls every domain and hands the addresses to the sink.
     *
     * @param runId   the run the addresses belong to
     * @param domains the domains to crawl
     * @param sink    where the addresses and the final status go
     * @return what happened
     */
    public ExtractionOutcome run(Long runId, List<DomainEntry> domains, ExtractionSink sink) {
        CrawlResult result;
        try {
            if (domains == null || domains.isEmpty()) {
                result = new CrawlResult(ExtractionStatus.FAILED, "the run has no domains to crawl", 0, 0, 0);
            } else {
                result = crawl(domains, sink);
            }
        } catch (RuntimeException e) {
            LOG.error("The extraction run {} failed ({})", runId, e.getClass().getSimpleName());
            result = new CrawlResult(
                    ExtractionStatus.FAILED,
                    "the run failed (" + e.getClass().getSimpleName() + ")",
                    0,
                    domains == null ? 0 : domains.size(),
                    0);
        }
        try {
            sink.complete(result.status(), result.detail());
        } catch (RuntimeException e) {
            LOG.error("The extraction run {} could not be closed ({})", runId, e.getClass().getSimpleName());
        }
        return new ExtractionOutcome(
                result.status(),
                domains == null ? 0 : domains.size(),
                result.succeeded(),
                result.failed(),
                result.addresses());
    }

    private CrawlResult crawl(List<DomainEntry> domains, ExtractionSink sink) {
        Executor executor = executor();
        int chunkSize = Math.max(1, properties.workerThreads());
        int writeBatchSize = Math.max(1, properties.writeBatchSize());
        Set<String> seen = ConcurrentHashMap.newKeySet();
        AtomicInteger failed = new AtomicInteger();
        AtomicInteger succeeded = new AtomicInteger();
        List<FoundAddress> batch = new ArrayList<>();
        Object batchLock = new Object();

        for (int start = 0; start < domains.size(); start += chunkSize) {
            List<DomainEntry> chunk = domains.subList(start, Math.min(start + chunkSize, domains.size()));
            CountDownLatch latch = new CountDownLatch(chunk.size());
            for (DomainEntry entry : chunk) {
                executor.execute(() -> {
                    try {
                        crawlOne(entry, sink, seen, failed, succeeded, batch, batchLock, writeBatchSize);
                    } catch (RuntimeException e) {
                        failed.incrementAndGet();
                        LOG.warn("The crawl of {} failed ({})", LogSafe.text(entry.domain()), e.getClass().getSimpleName());
                    } finally {
                        latch.countDown();
                    }
                });
            }
            try {
                long budget = (long) Math.max(1, properties.perDomainTimeoutSeconds()) * chunk.size() + 30L;
                if (!latch.await(budget, TimeUnit.SECONDS)) {
                    LOG.warn("A chunk of {} domains did not finish in {} seconds", chunk.size(), budget);
                    failed.addAndGet(chunk.size());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                failed.incrementAndGet();
                break;
            }
        }

        synchronized (batchLock) {
            if (!batch.isEmpty()) {
                sink.storeAddresses(List.copyOf(batch));
                batch.clear();
            }
        }

        int failedDomains = failed.get();
        int foundDomains = succeeded.get();
        ExtractionStatus status;
        if (failedDomains == 0) {
            status = ExtractionStatus.SUCCESS;
        } else if (foundDomains > 0) {
            status = ExtractionStatus.PARTIAL;
        } else {
            status = ExtractionStatus.FAILED;
        }
        String detail = failedDomains == 0
                ? null
                : failedDomains + " of " + domains.size() + " domains could not be crawled";
        return new CrawlResult(status, detail, foundDomains, failedDomains, seen.size());
    }

    /**
     * Crawls one domain and adds what it found to the batch.
     */
    private void crawlOne(
            DomainEntry entry,
            ExtractionSink sink,
            Set<String> seen,
            AtomicInteger failed,
            AtomicInteger succeeded,
            List<FoundAddress> batch,
            Object batchLock,
            int writeBatchSize) {
        List<String> addresses = crawler.findEmails(entry.domain());
        if (addresses == null || addresses.isEmpty()) {
            return;
        }
        succeeded.incrementAndGet();
        List<FoundAddress> found = new ArrayList<>();
        for (String address : addresses) {
            String normalised = normalise(address);
            if (normalised == null || !seen.add(normalised)) {
                continue;
            }
            found.add(new FoundAddress(entry.domain(), normalised, sha256(normalised), entry.sourceRow()));
        }
        if (found.isEmpty()) {
            return;
        }
        synchronized (batchLock) {
            batch.addAll(found);
            if (batch.size() >= writeBatchSize) {
                sink.storeAddresses(List.copyOf(batch));
                batch.clear();
            }
        }
    }

    /**
     * The executor the work runs on.
     *
     * <p>The application owns a bounded executor named {@code extractionExecutor}. When it
     * is not there the worker falls back to a bounded pool of its own, so the fan-out is
     * bounded either way.
     */
    private Executor executor() {
        Executor executor = executorProvider.getIfAvailable();
        if (executor != null) {
            return executor;
        }
        LOG.warn("The extraction executor is not configured, the worker uses its own bounded pool");
        Executor fallback = fallbackExecutor;
        if (fallback == null) {
            int threads = Math.max(1, properties.workerThreads());
            ThreadPoolExecutor pool = new ThreadPoolExecutor(
                    threads,
                    threads,
                    60L,
                    TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(Math.max(1, properties.queueCapacity())),
                    new ThreadPoolExecutor.AbortPolicy());
            pool.allowCoreThreadTimeOut(true);
            fallback = pool;
            fallbackExecutor = pool;
        }
        return fallback;
    }

    private static String normalise(String address) {
        if (address == null) {
            return null;
        }
        String normalised = address.trim().toLowerCase(Locale.ROOT);
        if (normalised.isEmpty() || normalised.length() > 254 || !normalised.contains("@")) {
            return null;
        }
        return normalised;
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /** One domain of the run, with the row it came from. */
    public record DomainEntry(String domain, Integer sourceRow) {}

    /** One address that was found. */
    public record FoundAddress(String domain, String email, byte[] emailHash, Integer sourceRow) {}

    /** What a run did. */
    public record ExtractionOutcome(
            ExtractionStatus status, int domainsTotal, int domainsSucceeded, int domainsFailed, int addressesFound) {}

    /** The result of the crawl itself, before the run is closed. */
    private record CrawlResult(
            ExtractionStatus status, String detail, int succeeded, int failed, int addresses) {}
}
