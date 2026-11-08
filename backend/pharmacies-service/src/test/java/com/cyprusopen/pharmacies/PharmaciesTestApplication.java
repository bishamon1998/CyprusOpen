package com.cyprusopen.pharmacies;

import java.time.Clock;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** Starts the pharmacies module on its own for its integration tests; production runs it inside backend/app. */
@SpringBootApplication
class PharmaciesTestApplication {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
