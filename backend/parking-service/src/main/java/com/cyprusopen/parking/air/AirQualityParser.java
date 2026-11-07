package com.cyprusopen.parking.air;

import static com.cyprusopen.parking.air.AirQualityConstants.MAX_STATIONS;
import static com.cyprusopen.parking.air.AirQualityConstants.SOURCE_ZONE;

import com.cyprusopen.common.geo.CyprusBounds;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads {@code {"data": {"station_1": {name_en, name_el, type_en, latitude, longitude, pollutants: {date_time,
 * pollutant_N: {code, value}}}}}}. Unknown pollutants are ignored; a station without coordinates in Cyprus,
 * a measurement time or any kept reading is skipped. Throws {@link IllegalArgumentException} when nothing usable remains.
 */
@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class AirQualityParser {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final JsonMapper mapper;

    AirQualitySnapshot parse(byte[] body, Instant retrievedAt) {
        JsonNode root;
        try {
            root = mapper.readTree(body);
        } catch (JacksonException invalid) {
            throw new IllegalArgumentException("Air quality response is not JSON", invalid);
        }
        var data = root.path("data");
        if (!data.isObject()) throw new IllegalArgumentException("Air quality response has no data");
        var stations = new ArrayList<AirQualitySnapshot.Station>();
        for (var entry : data.properties()) {
            if (stations.size() == MAX_STATIONS) throw new IllegalArgumentException("Too many stations");
            station(entry.getKey(), entry.getValue()).ifPresent(stations::add);
        }
        if (stations.isEmpty()) throw new IllegalArgumentException("No usable air quality stations");
        return new AirQualitySnapshot(retrievedAt, stations);
    }

    private static Optional<AirQualitySnapshot.Station> station(String id, JsonNode node) {
        var latitude = number(node.path("latitude"));
        var longitude = number(node.path("longitude"));
        var name = node.path("name_en").asString("").strip();
        if (name.isEmpty() || !CyprusBounds.contains(latitude, longitude)) return Optional.empty();
        var pollutants = node.path("pollutants");
        Instant measuredAt;
        try {
            measuredAt = LocalDateTime.parse(pollutants.path("date_time").asString(""), TIME).atZone(SOURCE_ZONE).toInstant();
        } catch (DateTimeParseException missing) {
            return Optional.empty();
        }
        var readings = new LinkedHashMap<String, Double>();
        for (var reading : pollutants.properties()) {
            if (!reading.getValue().isObject()) continue;
            var value = number(reading.getValue().path("value"));
            Pollutant.byCode(reading.getValue().path("code").asString(""))
                    .filter(pollutant -> Double.isFinite(value) && value >= 0)
                    .ifPresent(pollutant -> readings.put(pollutant.key(), value));
        }
        if (readings.isEmpty()) return Optional.empty();
        return Optional.of(new AirQualitySnapshot.Station(id, name, node.path("name_el").asString("").strip(),
                node.path("type_en").asString("").strip(), latitude, longitude, measuredAt, readings));
    }

    /** The source sends numbers as strings ("35.15"); anything else is NaN. */
    private static double number(JsonNode node) {
        if (node.isNumber()) return node.asDouble();
        try {
            return Double.parseDouble(node.asString(""));
        } catch (NumberFormatException notANumber) {
            return Double.NaN;
        }
    }
}
