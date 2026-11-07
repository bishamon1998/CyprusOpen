package com.cyprusopen.places;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * The latest successful import of one source, kept as a single JSON document (row id 1) in a table with the
 * columns {@code id}, {@code document} (JSONB) and {@code retrieved_at}. A failed refresh never calls
 * {@link #replace}, so the previous document stays.
 */
public abstract class JsonDocumentStore<T> {
    private static final int ID = 1;

    private final JdbcClient jdbc;
    private final JsonMapper mapper;
    private final Class<T> type;
    private final String read;
    private final String replace;
    private final String retrieved;

    /** {@code table} is a constant of the subclass, never input, so it is safe to place in the SQL. */
    protected JsonDocumentStore(JdbcClient jdbc, JsonMapper mapper, String table, Class<T> type) {
        if (!table.matches("[a-z_]+")) throw new IllegalArgumentException("Invalid table name " + table);
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.type = type;
        read = "SELECT document FROM " + table + " WHERE id = :id";
        replace = """
                INSERT INTO %s (id, document, retrieved_at) VALUES (:id, CAST(:document AS JSONB), :retrieved)
                ON CONFLICT (id) DO UPDATE SET document = EXCLUDED.document, retrieved_at = EXCLUDED.retrieved_at
                """.formatted(table);
        retrieved = "SELECT retrieved_at FROM " + table + " WHERE id = :id";
    }

    public Optional<T> read() {
        return jdbc.sql(read).param("id", ID).query((rs, row) -> mapper.readValue(rs.getString(1), type)).optional();
    }

    public void replace(T document, Instant retrievedAt) {
        jdbc.sql(replace).param("id", ID).param("document", mapper.writeValueAsString(document))
                .param("retrieved", Timestamp.from(retrievedAt)).update();
    }

    /** True when nothing is stored yet or the last import is older than {@code maxAge}: the start-up refresh rule. */
    public boolean olderThan(Duration maxAge, Instant now) {
        return jdbc.sql(retrieved).param("id", ID).query((rs, row) -> rs.getTimestamp(1).toInstant()).optional()
                .map(retrievedAt -> retrievedAt.plus(maxAge).isBefore(now)).orElse(true);
    }
}
