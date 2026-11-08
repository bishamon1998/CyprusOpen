package com.cyprusopen.app;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Runs the statistics, places and pharmacies modules in one process to keep hosting small. The modules stay
 * separate: own packages, databases, roles, Liquibase changelogs, transactions and API paths.
 */
@SpringBootApplication(scanBasePackages = {
        "com.cyprusopen.statistics", "com.cyprusopen.places", "com.cyprusopen.pharmacies"})
public class BackendApplication {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
