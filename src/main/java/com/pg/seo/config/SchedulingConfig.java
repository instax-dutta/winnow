package com.pg.seo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ErrorHandler;

/**
 * The scheduler pool.
 *
 * <p>The pool has more than one thread. The original used a single thread, so one stalled
 * IMAP connection delayed every other job behind it.
 *
 * <p>A job that throws is logged and the schedule continues. Without an error handler the
 * exception would cancel the recurring task and the job would silently stop running.
 */
@Configuration
public class SchedulingConfig {

    /** The number of threads that run scheduled jobs. */
    private static final int POOL_SIZE = 4;

    @Bean(name = "taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(POOL_SIZE);
        scheduler.setThreadNamePrefix("seo-sched-");
        scheduler.setErrorHandler(new LoggingErrorHandler());
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }

    /** Logs a failed job and lets the schedule carry on. */
    static final class LoggingErrorHandler implements ErrorHandler {

        private static final Logger log = LoggerFactory.getLogger(LoggingErrorHandler.class);

        @Override
        public void handleError(Throwable throwable) {
            log.error("A scheduled job failed and was rescheduled", throwable);
        }
    }
}
