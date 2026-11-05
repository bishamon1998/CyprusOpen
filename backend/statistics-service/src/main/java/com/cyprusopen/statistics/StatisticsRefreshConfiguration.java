package com.cyprusopen.statistics;

import static com.cyprusopen.statistics.StatisticsConstants.SCHEDULE_ZONE;
import static com.cyprusopen.statistics.StatisticsConstants.WEEKLY_CRON;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Imports missing datasets on start-up and checks CYSTAT for new releases every week. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "statistics.sync-enabled", havingValue = "true")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
class StatisticsRefreshConfiguration {
    private final StatisticsRefresh refresh;

    @EventListener(ApplicationReadyEvent.class)
    void initialize() {
        if (refresh.anyMissing()) log.info("Initial statistics refresh: {}", refresh.refreshAll());
    }

    @Scheduled(cron = WEEKLY_CRON, zone = SCHEDULE_ZONE)
    void weekly() {
        log.info("Statistics refresh: {}", refresh.refreshAll());
    }
}
