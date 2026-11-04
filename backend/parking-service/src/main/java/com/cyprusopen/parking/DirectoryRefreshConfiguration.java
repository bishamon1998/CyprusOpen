package com.cyprusopen.parking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
class DirectoryRefreshConfiguration {
    @Bean
    @Profile("directory-refresh")
    ApplicationRunner refreshOnce(DirectorySync sync) {
        return arguments -> {
            var result = sync.refresh();
            org.slf4j.LoggerFactory.getLogger(DirectorySync.class).info("Directory refresh: {}", result);
            if ("invalid".equals(result) || "unavailable".equals(result)) throw new IllegalStateException("Directory refresh failed: " + result);
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

        @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
        void initialize() {
            if (directory.snapshot().items().isEmpty()) log.info("Initial directory refresh: {}", sync.refresh());
        }

        @Scheduled(cron = "0 0 3 1 * *", zone = "UTC")
        void refresh() { log.info("Directory refresh: {}", sync.refresh()); }
    }
}
