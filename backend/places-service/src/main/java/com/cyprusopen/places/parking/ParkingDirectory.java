package com.cyprusopen.places.parking;

import static com.cyprusopen.places.parking.DirectoryConstants.CATALOG_URL;
import static com.cyprusopen.places.parking.DirectoryConstants.DIRECTORY_ID;
import static com.cyprusopen.places.parking.DirectoryConstants.LICENSE;
import static com.cyprusopen.places.parking.DirectoryConstants.LICENSE_URL;
import static com.cyprusopen.places.parking.DirectoryConstants.RESOURCE_URL;
import static com.cyprusopen.places.parking.DirectoryConstants.SOURCE_NAME;
import static com.cyprusopen.places.parking.DirectoryConstants.TRANSFORMATIONS;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import com.cyprusopen.places.PlacesDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** Persists the single normalized parking directory document. Validation belongs to {@link DirectoryXmlParser}. */
@Repository
@DependsOn(PlacesDatabase.LIQUIBASE)
class ParkingDirectory {
    private final JdbcClient jdbc;
    private final JsonMapper mapper;

    @Autowired
    ParkingDirectory(@Qualifier(PlacesDatabase.JDBC) JdbcClient jdbc, JsonMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    Snapshot snapshot() {
        return read().snapshot();
    }

    Read read() {
        return statement(DirectorySql.READ).query((rs, row) -> {
            var document = rs.getString(1);
            var checked = rs.getTimestamp(2);
            var stored = document == null ? null : mapper.readValue(document, Snapshot.class);
            var snapshot = stored == null ? Snapshot.of(null, null, List.of())
                    : Snapshot.of(stored.publishedAt(), stored.retrievedAt(), stored.items());
            return new Read(snapshot, checked == null ? null : checked.toInstant());
        }).single();
    }

    void replace(Snapshot snapshot, String etag, String modified) {
        statement(DirectorySql.REPLACE)
                .param("document", mapper.writeValueAsString(snapshot))
                .param("checked", Timestamp.from(snapshot.retrievedAt()))
                .param("etag", etag).param("modified", modified).update();
    }

    /** Validators of the stored directory; none while it is empty, so the next request fetches it in full. */
    Validators validators() {
        return statement(DirectorySql.VALIDATORS)
                .query((rs, row) -> new Validators(rs.getString(1), rs.getString(2))).optional()
                .orElse(new Validators(null, null));
    }

    void markUnchanged(Instant checked) {
        statement(DirectorySql.MARK_UNCHANGED).param("checked", Timestamp.from(checked)).update();
    }

    private JdbcClient.StatementSpec statement(String sql) {
        return jdbc.sql(sql).param("id", DIRECTORY_ID);
    }

    record Snapshot(String source, String catalogUrl, String resourceUrl, String license,
                    String licenseUrl, Instant publishedAt, Instant retrievedAt,
                    String transformations, List<Location> items) {
        /**
         * A snapshot with the current attribution. The stored document keeps a copy, but attribution always
         * comes from {@link DirectoryConstants}, so a correction does not wait for the next source publication.
         */
        static Snapshot of(Instant publishedAt, Instant retrievedAt, List<Location> items) {
            return new Snapshot(SOURCE_NAME, CATALOG_URL, RESOURCE_URL, LICENSE, LICENSE_URL,
                    publishedAt, retrievedAt, TRANSFORMATIONS, items);
        }
    }
    record Location(String id, String name, double latitude, double longitude) {}
    record Read(Snapshot snapshot, Instant checkedAt) {}
    record Validators(String etag, String modified) {}
}
