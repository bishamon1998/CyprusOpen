package com.cyprusopen.parking;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Component
@DependsOn("liquibase")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ParkingDirectory {
    private final JdbcClient jdbc;
    private final JsonMapper mapper;

    @Transactional(readOnly = true)
    public Snapshot snapshot() {
        return read().snapshot();
    }

    Read read() {
        return jdbc.sql("SELECT document::text,last_checked_at FROM parking_directory WHERE id=1")
                .query((rs, row) -> new Read(rs.getString(1) == null ? empty() : mapper.readValue(rs.getString(1), Snapshot.class), rs.getTimestamp(2) == null ? null : rs.getTimestamp(2).toInstant())).single();
    }

    private static Snapshot empty() {
        return new Snapshot("Cyprus National Access Point / Public Works Department", "https://www.traffic4cyprus.org.cy/en/dataset/parking_areas",
                "https://fixcyprus.cy/gnosis/open/api/nap/datasets/parking_areas/", "CC BY 4.0", "https://creativecommons.org/licenses/by/4.0/",
                null, null, "English names only; missing/non-English names and OSM Origin placeholders are unnamed. Coordinates and identifiers preserved.", List.of());
    }

    void replace(Snapshot snapshot, String etag, String modified) {
        validate(snapshot);
        jdbc.sql("UPDATE parking_directory SET document=CAST(:document AS jsonb),last_checked_at=:checked,etag=:etag,last_modified=:modified,consecutive_failures=0,sync_status='updated' WHERE id=1")
            .param("document", mapper.writeValueAsString(snapshot))
            .param("checked", java.sql.Timestamp.from(snapshot.retrievedAt()))
            .param("etag", etag).param("modified", modified).update();
    }

    static void validate(Snapshot snapshot) {
        var identifiers = new HashSet<String>();
        if (snapshot.items().isEmpty() || snapshot.items().size() > 20000 || snapshot.publishedAt() == null
                || snapshot.publishedAt().isAfter(Instant.now().plusSeconds(300))) throw new IllegalArgumentException("Invalid directory publication");
        for (var place : snapshot.items()) {
            if (place.id() == null || !identifiers.add(place.id()) || !Double.isFinite(place.latitude())
                    || !Double.isFinite(place.longitude()) || place.latitude() < 34 || place.latitude() > 36
                    || place.longitude() < 32 || place.longitude() > 35) throw new IllegalArgumentException("Invalid parking location");
        }
    }

    record Snapshot(String source, String catalogUrl, String resourceUrl, String license,
                    String licenseUrl, Instant publishedAt, Instant retrievedAt,
                    String transformations, List<Location> items) {}
    record Location(String id, String name, double latitude, double longitude) {}
    record Read(Snapshot snapshot, Instant checkedAt) {}
}
