package io.github.instaxdutta.winnow.extraction;

import io.github.instaxdutta.winnow.domain.ExtractionStatus;
import java.util.List;

/**
 * Where the results of an extraction go.
 *
 * <p>The extraction package never writes to the database itself, so whoever starts a run
 * supplies the sink. The sink is called from the worker thread that found the addresses,
 * which means it has to be safe to call from several threads at once.
 */
public interface ExtractionSink {

    /**
     * Stores one batch of addresses.
     *
     * <p>The batch holds at most the configured write batch size and never holds the same
     * address twice.
     */
    void storeAddresses(List<ExtractionWorker.FoundAddress> batch);

    /**
     * Ends the run.
     *
     * <p>This is always called, exactly once, with a status that is never {@code QUEUED}
     * and never {@code RUNNING}.
     */
    void complete(ExtractionStatus status, String errorDetail);
}
