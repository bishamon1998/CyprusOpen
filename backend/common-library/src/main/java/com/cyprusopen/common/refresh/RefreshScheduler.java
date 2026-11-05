package com.cyprusopen.common.refresh;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;

/**
 * Runs every {@link RefreshJob} of a service from the shared schedule: once after start-up when its data
 * is missing or stale, then on its cron. Logs the schedule at start-up and the outcome of every run.
 */
@Slf4j
public class RefreshScheduler implements ApplicationRunner, DisposableBean {
    private final Map<String, RefreshJob> jobs = new LinkedHashMap<>();
    private final RefreshProperties properties;
    private ThreadPoolTaskScheduler scheduler;

    public RefreshScheduler(List<RefreshJob> jobs, RefreshProperties properties) {
        this.properties = properties;
        for (var job : jobs) {
            var schedule = properties.jobs().get(job.name());
            if (schedule == null || schedule.cron() == null || !CronExpression.isValidExpression(schedule.cron())) {
                throw new IllegalStateException("Missing or invalid schedule for refresh job " + job.name()
                        + " in cyprus-open-refresh.properties");
            }
            if (this.jobs.put(job.name(), job) != null) throw new IllegalStateException("Duplicate job " + job.name());
        }
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (properties.runOnce() != null) {
            var job = jobs.get(properties.runOnce());
            if (job == null) throw new IllegalStateException("Unknown refresh job " + properties.runOnce());
            var result = execute(job);
            if (result.failed()) throw new IllegalStateException("Refresh " + job.name() + " failed: " + result);
            return;
        }
        jobs.values().forEach(this::start);
    }

    /** Runs a job now on the calling thread, logging its outcome and duration. */
    public synchronized RefreshResult execute(RefreshJob job) {
        var started = System.nanoTime();
        RefreshResult result;
        try {
            result = job.refresh();
        } catch (RuntimeException failure) {
            log.error("Refresh {} failed unexpectedly", job.name(), failure);
            result = RefreshResult.UNAVAILABLE;
        }
        log.info("Refresh {}: {} in {} ms", job.name(), result, (System.nanoTime() - started) / 1_000_000);
        return result;
    }

    private void start(RefreshJob job) {
        var schedule = properties.jobs().get(job.name());
        if (!schedule.enabled()) {
            log.info("Refresh {}: disabled", job.name());
            return;
        }
        var cron = CronExpression.parse(schedule.cron());
        log.info("Refresh {}: cron '{}' {}, next run {}", job.name(), schedule.cron(), properties.zone(),
                cron.next(ZonedDateTime.now(properties.zone())));
        var tasks = scheduler();
        if (job.needsInitialRun()) tasks.schedule(() -> execute(job), Instant.now());
        tasks.schedule(() -> execute(job), new CronTrigger(schedule.cron(), properties.zone()));
    }

    private ThreadPoolTaskScheduler scheduler() {
        if (scheduler == null) {
            scheduler = new ThreadPoolTaskScheduler();
            scheduler.setThreadNamePrefix("refresh-");
            scheduler.initialize();
        }
        return scheduler;
    }

    @Override
    public void destroy() {
        if (scheduler != null) scheduler.shutdown();
    }
}
