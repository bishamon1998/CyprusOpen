package com.cyprusopen.parking;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
class ParkingDirectory {
    private final Snapshot snapshot;

    ParkingDirectory() throws IOException {
        try (var input = new ClassPathResource("parking-directory.json").getInputStream()) {
            snapshot = JsonMapper.builder().build().readValue(input, Snapshot.class);
        }
        var identifiers = new HashSet<String>();
        if (snapshot.items().isEmpty()) throw new IllegalStateException("Empty parking directory");
        for (var place : snapshot.items()) {
            if (!identifiers.add(place.id()) || !Double.isFinite(place.latitude())
                    || !Double.isFinite(place.longitude()) || Math.abs(place.latitude()) > 90
                    || Math.abs(place.longitude()) > 180) {
                throw new IllegalStateException("Invalid parking directory");
            }
        }
    }

    Snapshot snapshot() { return snapshot; }

    record Snapshot(String source, String catalogUrl, String resourceUrl, String license,
                    String licenseUrl, Instant publishedAt, Instant retrievedAt,
                    String transformations, List<Location> items) {}
    record Location(String id, String name, double latitude, double longitude) {}
}
