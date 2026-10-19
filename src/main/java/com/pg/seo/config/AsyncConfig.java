package com.pg.seo.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * The worker pools.
 *
 * <p>Every pool is named, bounded and has an explicit rejection policy. The original declared
 * its {@code taskExecutor} method without {@code @Bean}, so the pool it described never
 * existed and the work silently ran on the unbounded common pool.
 *
 * <p>The rejection policy is {@link ThreadPoolExecutor.CallerRunsPolicy}: when a queue is
 * full the submitting thread runs the task itself. That slows the producer down instead of
 * dropping the work, and the producer is a request thread that has to answer anyway.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /** The number of threads that dispatch queued mail. */
    private static final int MAIL_DISPATCH_THREADS = 4;

    /** The number of queued mail tasks that may wait. */
    private static final int MAIL_DISPATCH_QUEUE = 200;

    private final ExtractionProperties extractionProperties;

    public AsyncConfig(ExtractionProperties extractionProperties) {
        this.extractionProperties = extractionProperties;
    }

    @Bean(name = "extractionExecutor")
    public ThreadPoolTaskExecutor extractionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(extractionProperties.workerThreads());
        executor.setMaxPoolSize(extractionProperties.workerThreads());
        executor.setQueueCapacity(extractionProperties.queueCapacity());
        executor.setThreadNamePrefix("seo-extract-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }

    @Bean(name = "mailDispatchExecutor")
    public ThreadPoolTaskExecutor mailDispatchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(MAIL_DISPATCH_THREADS);
        executor.setMaxPoolSize(MAIL_DISPATCH_THREADS);
        executor.setQueueCapacity(MAIL_DISPATCH_QUEUE);
        executor.setThreadNamePrefix("seo-mail-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }

    /**
     * The executor an {@code @Async} method without a qualifier runs on.
     *
     * <p>Without this the framework falls back to an unbounded pool, which is exactly what
     * the hard rules forbid.
     */
    @Override
    public Executor getAsyncExecutor() {
        return extractionExecutor();
    }
}
