package com.cyprusopen.places.air;

import static com.cyprusopen.places.air.AirQualityConstants.CATALOG_URL;
import static com.cyprusopen.places.air.AirQualityConstants.HISTORY;
import static com.cyprusopen.places.air.AirQualityConstants.INDEX_NAME;
import static com.cyprusopen.places.air.AirQualityConstants.INDEX_URL;
import static com.cyprusopen.places.air.AirQualityConstants.LICENSE;
import static com.cyprusopen.places.air.AirQualityConstants.LICENSE_URL;
import static com.cyprusopen.places.air.AirQualityConstants.SOURCE_NAME;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Current air quality by station with the European Air Quality Index level of each pollutant and of the station
 * (its worst pollutant), and each station's hourly readings of the last week. Serves stored data only; an empty
 * list means nothing has been imported yet.
 */
@RestController
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class AirQualityController {
    private static final Pattern STATION_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private final AirQualityStore store;
    private final AirQualityHistory history;

    @GetMapping("/api/v1/air-quality")
    ResponseEntity<Response> airQuality() {
        var snapshot = store.read();
        var stations = snapshot.map(stored -> stored.stations().stream().map(AirQualityController::station).toList())
                .orElse(List.of());
        var source = new Source(SOURCE_NAME, CATALOG_URL, LICENSE, LICENSE_URL, INDEX_NAME, INDEX_URL,
                newest(snapshot.orElse(null)), snapshot.map(AirQualitySnapshot::retrievedAt).orElse(null));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Response(source, stations));
    }

    /**
     * The station's hourly readings of the week before its newest import, oldest first. An unknown station has
     * none; the list starts when the site began collecting, so it can be shorter than a week.
     */
    @GetMapping("/api/v1/air-quality/stations/{id}/history")
    ResponseEntity<History> history(@PathVariable String id) {
        if (!STATION_ID.matcher(id).matches()) return ResponseEntity.notFound().build();
        var latest = store.read().flatMap(stored -> stored.stations().stream()
                .filter(station -> station.id().equals(id)).findFirst().map(AirQualitySnapshot.Station::measuredAt));
        var hours = latest.map(newest -> history.station(id, newest.minus(HISTORY)).stream()
                .map(hour -> new Hour(hour.measuredAt(), level(hour.readings()), readings(hour.readings()))).toList())
                .orElse(List.of());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new History(id, hours));
    }

    /** The newest station reading is how current the data is; retrievedAt is the last successful download. */
    private static Instant newest(AirQualitySnapshot snapshot) {
        if (snapshot == null) return null;
        return snapshot.stations().stream().map(AirQualitySnapshot.Station::measuredAt)
                .max(Comparator.naturalOrder()).orElse(null);
    }

    private static Station station(AirQualitySnapshot.Station stored) {
        return new Station(stored.id(), Map.of("en", stored.nameEn(), "el", stored.nameEl().isEmpty() ? stored.nameEn() : stored.nameEl()),
                stored.type(), stored.latitude(), stored.longitude(), stored.measuredAt(), level(stored.readings()),
                readings(stored.readings()));
    }

    private static List<Reading> readings(Map<String, Double> values) {
        return Arrays.stream(Pollutant.values())
                .filter(pollutant -> values.containsKey(pollutant.key()))
                .map(pollutant -> {
                    var value = values.get(pollutant.key());
                    return new Reading(pollutant.key(), value, pollutant.level(value).map(AirQualityLevel::key).orElse(null));
                }).toList();
    }

    /** The station level is that of its worst pollutant. */
    private static String level(Map<String, Double> values) {
        return Arrays.stream(Pollutant.values())
                .filter(pollutant -> values.containsKey(pollutant.key()))
                .flatMap(pollutant -> pollutant.level(values.get(pollutant.key())).stream())
                .max(Comparator.naturalOrder()).map(AirQualityLevel::key).orElse(null);
    }

    record Response(Source source, List<Station> stations) {}
    record Source(String name, String url, String license, String licenseUrl, String index, String indexUrl,
                  Instant measuredAt, Instant retrievedAt) {}
    record Station(String id, Map<String, String> name, String type, double latitude, double longitude,
                   Instant measuredAt, String level, List<Reading> readings) {}
    record Reading(String pollutant, double value, String level) {}
    record History(String stationId, List<Hour> hours) {}
    record Hour(Instant measuredAt, String level, List<Reading> readings) {}
}
