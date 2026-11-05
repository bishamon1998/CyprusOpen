package com.cyprusopen.common.refresh;

import java.time.ZoneId;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Schedules of all refresh jobs, bound from {@code cyprus-open-refresh.properties}.
 *
 * @param zone    time zone for every cron expression
 * @param jobs    schedule per job name
 * @param runOnce job to run a single time instead of scheduling (the process then exits); null otherwise
 */
@ConfigurationProperties("cyprusopen.refresh")
public record RefreshProperties(ZoneId zone, Map<String, Schedule> jobs, String runOnce) {
    public RefreshProperties {
        jobs = jobs == null ? Map.of() : Map.copyOf(jobs);
    }

    /** @param cron Spring cron: second minute hour day-of-month month day-of-week */
    public record Schedule(String cron, boolean enabled) {}
}
