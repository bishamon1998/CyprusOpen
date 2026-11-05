package com.cyprusopen.parking;

import static com.cyprusopen.parking.DirectoryConstants.INVALID;
import static com.cyprusopen.parking.DirectoryConstants.MONTHLY_CRON;
import static com.cyprusopen.parking.DirectoryConstants.SCHEDULE_ZONE;
import static com.cyprusopen.parking.DirectoryConstants.UNAVAILABLE;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@Slf4j
class DirectoryRefreshConfiguration {
    @Bean
    @Profile("directory-refresh")
    ApplicationRunner refreshOnce(DirectorySync sync) {
        return arguments -> {
            var result = sync.refresh();
            log.info("Directory refresh: {}", result);
            if (INVALID.equals(result) || UNAVAILABLE.equals(result)) {
                throw new IllegalStateException("Directory refresh failed: " + result);
            }
        };
    }

    @Configuration
    @EnableScheduling
    @Profile("!directory-refresh")
    @ConditionalOnProperty(name = "parking.directory.sync-enabled", havingValue = "true")
    @RequiredArgsConstructor(onConstructor_ = @Autowired)
    @Slf4j
    static class MonthlyRefresh {
        private final DirectorySync sync;
        private final ParkingDirectory directory;

        @EventListener(ApplicationReadyEvent.class)
        void initialize() {
            if (directory.snapshot().items().isEmpty()) {
                log.info("Initial directory refresh: {}", sync.refresh());
            }
        }

        @Scheduled(cron = MONTHLY_CRON, zone = SCHEDULE_ZONE)
        void refresh() {
            log.info("Directory refresh: {}", sync.refresh());
        }
    }
}
