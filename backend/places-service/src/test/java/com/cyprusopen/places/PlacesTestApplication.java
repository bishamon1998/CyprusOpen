package com.cyprusopen.places;

import java.time.Clock;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** Starts the places module on its own for its integration tests; production runs it inside backend/app. */
@SpringBootApplication
class PlacesTestApplication {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
