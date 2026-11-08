package com.cyprusopen.statistics;

import java.time.Clock;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** Starts the statistics module on its own for its integration tests; production runs it inside backend/app. */
@SpringBootApplication
class StatisticsTestApplication {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
