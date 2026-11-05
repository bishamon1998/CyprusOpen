package com.cyprusopen.pharmacies;

import static com.cyprusopen.pharmacies.PharmacyConstants.DAILY_CRON;
import static com.cyprusopen.pharmacies.PharmacyConstants.MAX_SCHEDULE_AGE;
import static com.cyprusopen.pharmacies.PharmacyConstants.SCHEDULE_ZONE;

import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "pharmacies.sync-enabled", havingValue = "true")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
class PharmacyRefreshConfiguration {
    private final PharmacyScheduleRefresh refresh;
    private final PharmacySchedules schedules;
    private final Clock clock;

    @EventListener(ApplicationReadyEvent.class)
    void initialize() {
        var checkedAt = schedules.checkedAt();
        if (checkedAt == null || checkedAt.isBefore(clock.instant().minus(MAX_SCHEDULE_AGE))) {
            log.info("Initial pharmacy schedule refresh: {}", refresh.refresh());
        }
    }

    @Scheduled(cron = DAILY_CRON, zone = SCHEDULE_ZONE)
    void daily() {
        log.info("Pharmacy schedule refresh: {}", refresh.refresh());
    }
}
