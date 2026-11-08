package com.cyprusopen.statistics;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Stores observations per dataset; a refresh replaces a dataset's rows atomically. */
@Repository
@DependsOn(StatisticsDatabase.LIQUIBASE)
class ObservationStore {
    private static final int BATCH_SIZE = 1_000;

    private final JdbcClient jdbc;
    private final JdbcTemplate batch;

    @Autowired
    ObservationStore(@Qualifier(StatisticsDatabase.JDBC) JdbcClient jdbc,
                     @Qualifier(StatisticsDatabase.BATCH) JdbcTemplate batch) {
        this.jdbc = jdbc;
        this.batch = batch;
    }

    Map<String, Imported> imported() {
        return jdbc.sql(StatisticsSql.DATASETS).query(ObservationStore::importedRow).stream()
                .collect(Collectors.toMap(Imported::code, Function.identity()));
    }

    Optional<Imported> imported(String code) {
        return jdbc.sql(StatisticsSql.DATASET).param("code", code).query(ObservationStore::importedRow).optional();
    }

    List<Observation> observations(String code) {
        return jdbc.sql(StatisticsSql.OBSERVATIONS).param("code", code)
                .query((rs, row) -> new Observation(rs.getString(1), rs.getString(2), rs.getDouble(3), rs.getBoolean(4)))
                .list();
    }

    @Transactional(StatisticsDatabase.TRANSACTIONS)
    void replace(String code, Instant sourceUpdated, Instant checked, List<Observation> observations) {
        jdbc.sql(StatisticsSql.UPSERT_DATASET).param("code", code).param("updated", Timestamp.from(sourceUpdated))
                .param("checked", Timestamp.from(checked)).update();
        jdbc.sql(StatisticsSql.DELETE_OBSERVATIONS).param("code", code).update();
        batch.batchUpdate(StatisticsSql.INSERT_OBSERVATION, observations, BATCH_SIZE, (statement, observation) -> {
            statement.setString(1, code);
            statement.setString(2, observation.series());
            statement.setString(3, observation.period());
            statement.setDouble(4, observation.value());
            statement.setBoolean(5, observation.lowReliability());
        });
    }

    void markChecked(String code, Instant checked) {
        jdbc.sql(StatisticsSql.MARK_CHECKED).param("code", code).param("checked", Timestamp.from(checked)).update();
    }

    private static Imported importedRow(ResultSet rs, int row) throws SQLException {
        return new Imported(rs.getString(1), rs.getTimestamp(2).toInstant(), rs.getTimestamp(3).toInstant());
    }

    record Imported(String code, Instant sourceUpdatedAt, Instant checkedAt) {}
}
