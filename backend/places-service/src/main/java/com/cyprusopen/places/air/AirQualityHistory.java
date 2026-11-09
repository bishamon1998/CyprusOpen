package com.cyprusopen.places.air;

import com.cyprusopen.places.PlacesDatabase;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Hourly readings of each station ({@code air_quality_history} table), collected from every import: the source
 * only publishes the current hour, so this is how the site can show the last days.
 */
@Repository
@DependsOn(PlacesDatabase.LIQUIBASE)
class AirQualityHistory {
    private static final TypeReference<Map<String, Double>> READINGS = new TypeReference<>() {};

    private final JdbcClient jdbc;
    private final JsonMapper mapper;

    @Autowired
    AirQualityHistory(@Qualifier(PlacesDatabase.JDBC) JdbcClient jdbc, JsonMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    record Hour(Instant measuredAt, Map<String, Double> readings) {}

    /** Adds each station's reading; an hour already stored (the source had not moved on) is kept as it was. */
    void record(AirQualitySnapshot snapshot) {
        for (var station : snapshot.stations()) {
            jdbc.sql("""
                    INSERT INTO air_quality_history (station_id, measured_at, readings)
                    VALUES (:station, :measured, CAST(:readings AS JSONB))
                    ON CONFLICT (station_id, measured_at) DO NOTHING
                    """).param("station", station.id()).param("measured", Timestamp.from(station.measuredAt()))
                    .param("readings", mapper.writeValueAsString(station.readings())).update();
        }
    }

    /** Removes readings measured before {@code cutoff}. */
    void removeBefore(Instant cutoff) {
        jdbc.sql("DELETE FROM air_quality_history WHERE measured_at < :cutoff")
                .param("cutoff", Timestamp.from(cutoff)).update();
    }

    /** The station's readings from {@code since} on, oldest first. */
    List<Hour> station(String id, Instant since) {
        return jdbc.sql("""
                SELECT measured_at, readings FROM air_quality_history
                WHERE station_id = :station AND measured_at >= :since ORDER BY measured_at
                """).param("station", id).param("since", Timestamp.from(since))
                .query((rs, row) -> new Hour(rs.getTimestamp(1).toInstant(), mapper.readValue(rs.getString(2), READINGS)))
                .list();
    }
}
