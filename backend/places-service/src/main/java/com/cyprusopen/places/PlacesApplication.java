package com.cyprusopen.places;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * The map tools in one service: parking locations ({@code parking}), beaches ({@code beach}) and air quality
 * ({@code air}). Each package has its own import, table and API; they share the database and refresh scheduler.
 */
@SpringBootApplication
public class PlacesApplication {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    public static void main(String[] args) {
        SpringApplication.run(PlacesApplication.class, args);
    }
}
