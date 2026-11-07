package com.cyprusopen.parking.beach;

import static com.cyprusopen.parking.beach.BeachConstants.SNAPSHOT_ID;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** Keeps only the latest successful snapshot; a failed refresh leaves it untouched. */
@Repository
@DependsOn("liquibase")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class BeachStore {
    private final JdbcClient jdbc;
    private final JsonMapper mapper;

    Optional<BeachSnapshot> read() {
        return jdbc.sql("SELECT document FROM beaches WHERE id = :id").param("id", SNAPSHOT_ID)
                .query((rs, row) -> mapper.readValue(rs.getString(1), BeachSnapshot.class)).optional();
    }

    void replace(BeachSnapshot snapshot) {
        jdbc.sql("""
                INSERT INTO beaches (id, document, retrieved_at) VALUES (:id, CAST(:document AS JSONB), :retrieved)
                ON CONFLICT (id) DO UPDATE SET document = EXCLUDED.document, retrieved_at = EXCLUDED.retrieved_at
                """).param("id", SNAPSHOT_ID).param("document", mapper.writeValueAsString(snapshot))
                .param("retrieved", Timestamp.from(snapshot.retrievedAt())).update();
    }

    Optional<Instant> retrievedAt() {
        return jdbc.sql("SELECT retrieved_at FROM beaches WHERE id = :id").param("id", SNAPSHOT_ID)
                .query((rs, row) -> rs.getTimestamp(1).toInstant()).optional();
    }
}
