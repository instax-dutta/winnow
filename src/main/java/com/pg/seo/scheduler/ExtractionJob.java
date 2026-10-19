package com.pg.seo.scheduler;

import com.pg.seo.domain.ExtractionResult;
import com.pg.seo.domain.ExtractionRun;
import com.pg.seo.domain.ExtractionStatus;
import com.pg.seo.extraction.ExtractionSink;
import com.pg.seo.extraction.ExtractionWorker;
import com.pg.seo.repository.ExtractionResultRepository;
import com.pg.seo.repository.ExtractionRunRepository;
import com.pg.seo.service.ExtractionService;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Advances the extraction runs that were left behind.
 *
 * <p>A run that is still {@code QUEUED} was never started: the service that creates a run
 * crawls the domains inline, so a run that is still queued means the application died
 * between the two steps. Such a run is closed as failed.
 *
 * <p>A run that is still {@code RUNNING} after the run timeout is resumed: the domains its
 * results already name are crawled again and the run is closed. The addresses that are
 * already stored are skipped, so the resume never stores the same address twice.
 *
 * <p>Either way the run ends in {@code SUCCESS}, {@code PARTIAL} or {@code FAILED}. It is
 * never left in {@code RUNNING}.
 *
 * <p>The run row is written through the service layer, so the scheduler never touches the
 * repository directly.
 */
@Component
public class ExtractionJob {

    private static final Logger LOG = LoggerFactory.getLogger(ExtractionJob.class);

    /** How many runs are read in one query. */
    private static final int RUN_PAGE_SIZE = 100;

    /** The most pages of runs one pass reads. */
    private static final int MAX_RUN_PAGES = 20;

    private final ExtractionRunRepository runs;
    private final ExtractionResultRepository results;
    private final ExtractionWorker worker;
    private final ExtractionService extractions;
    private final Duration runTimeout;

    public ExtractionJob(
            ExtractionRunRepository runs,
            ExtractionResultRepository results,
            ExtractionWorker worker,
            ExtractionService extractions,
            @Value("${seo.scheduler.extraction-run-timeout-minutes:30}") long runTimeoutMinutes) {
        this.runs = runs;
        this.results = results;
        this.worker = worker;
        this.extractions = extractions;
        this.runTimeout = Duration.ofMinutes(Math.max(runTimeoutMinutes, 1));
    }

    /**
     * Closes every run that was left behind.
     */
    @Scheduled(fixedDelayString = "${seo.scheduler.extraction-delay-ms:60000}")
    public void advance() {
        for (int page = 0; page < MAX_RUN_PAGES; page++) {
            List<ExtractionRun> pageRuns = runs.findAll(PageRequest.of(page, RUN_PAGE_SIZE)).getContent();
            if (pageRuns.isEmpty()) {
                return;
            }
            for (ExtractionRun run : pageRuns) {
                try {
                    advance(run);
                } catch (RuntimeException e) {
                    LOG.error("The extraction run {} could not be advanced ({})",
                            run.getId(), e.getClass().getSimpleName());
                }
            }
            if (pageRuns.size() < RUN_PAGE_SIZE) {
                return;
            }
        }
        LOG.warn("The extraction pass stopped after {} pages of runs", MAX_RUN_PAGES);
    }

    private void advance(ExtractionRun run) {
        if (run.getStatus() == ExtractionStatus.QUEUED) {
            close(run, ExtractionStatus.FAILED, "the run was queued and never started");
            return;
        }
        if (run.getStatus() != ExtractionStatus.RUNNING) {
            return;
        }
        if (run.getStartedAt() != null
                && run.getStartedAt().plus(runTimeout).isAfter(Instant.now())) {
            // Another worker may still be inside the run.
            return;
        }
        List<ExtractionWorker.DomainEntry> domains = domainsOf(run);
        LOG.info("Resuming the extraction run {} with {} domains", run.getId(), domains.size());
        worker.run(run.getId(), domains, extractions.sinkFor(run));
    }

    /**
     * The domains the run already knows about, with the row they came from.
     */
    private List<ExtractionWorker.DomainEntry> domainsOf(ExtractionRun run) {
        Map<String, Integer> rows = new LinkedHashMap<>();
        for (ExtractionResult result : results.findByRunId(run.getId())) {
            rows.putIfAbsent(result.getDomain(), result.getSourceRow());
        }
        List<ExtractionWorker.DomainEntry> domains = new ArrayList<>(rows.size());
        rows.forEach((domain, row) -> domains.add(new ExtractionWorker.DomainEntry(domain, row)));
        return domains;
    }

    private void close(ExtractionRun run, ExtractionStatus status, String detail) {
        extractions.closeRun(run, status, detail);
        LOG.info("The extraction run {} was closed as {}", run.getId(), status);
    }

}
