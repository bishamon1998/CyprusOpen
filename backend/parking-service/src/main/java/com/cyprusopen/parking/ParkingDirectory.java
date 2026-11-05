package com.cyprusopen.parking;

import static com.cyprusopen.parking.DirectoryConstants.CATALOG_URL;
import static com.cyprusopen.parking.DirectoryConstants.DIRECTORY_ID;
import static com.cyprusopen.parking.DirectoryConstants.LICENSE;
import static com.cyprusopen.parking.DirectoryConstants.LICENSE_URL;
import static com.cyprusopen.parking.DirectoryConstants.RESOURCE_URL;
import static com.cyprusopen.parking.DirectoryConstants.SOURCE_NAME;
import static com.cyprusopen.parking.DirectoryConstants.TRANSFORMATIONS;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** Persists the single normalized parking directory document. Validation belongs to {@link DirectoryXmlParser}. */
@Repository
@DependsOn("liquibase")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ParkingDirectory {
    private final JdbcClient jdbc;
    private final JsonMapper mapper;

    Snapshot snapshot() {
        return read().snapshot();
    }

    Read read() {
        return statement(DirectorySql.READ).query((rs, row) -> {
            var document = rs.getString(1);
            var checked = rs.getTimestamp(2);
            return new Read(document == null ? empty() : mapper.readValue(document, Snapshot.class),
                    checked == null ? null : checked.toInstant());
        }).single();
    }

    void replace(Snapshot snapshot, String etag, String modified) {
        statement(DirectorySql.REPLACE)
                .param("document", mapper.writeValueAsString(snapshot))
                .param("checked", Timestamp.from(snapshot.retrievedAt()))
                .param("etag", etag).param("modified", modified).update();
    }

    Validators validators() {
        return statement(DirectorySql.VALIDATORS)
                .query((rs, row) -> new Validators(rs.getString(1), rs.getString(2))).single();
    }

    void markUnchanged() {
        statement(DirectorySql.MARK_UNCHANGED).update();
    }

    private JdbcClient.StatementSpec statement(String sql) {
        return jdbc.sql(sql).param("id", DIRECTORY_ID);
    }

    private static Snapshot empty() {
        return new Snapshot(SOURCE_NAME, CATALOG_URL, RESOURCE_URL, LICENSE, LICENSE_URL,
                null, null, TRANSFORMATIONS, List.of());
    }

    record Snapshot(String source, String catalogUrl, String resourceUrl, String license,
                    String licenseUrl, Instant publishedAt, Instant retrievedAt,
                    String transformations, List<Location> items) {}
    record Location(String id, String name, double latitude, double longitude) {}
    record Read(Snapshot snapshot, Instant checkedAt) {}
    record Validators(String etag, String modified) {}
}
