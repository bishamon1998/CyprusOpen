package com.cyprusopen.places.air;

import static com.cyprusopen.places.air.AirQualityConstants.CATALOG_URL;
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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Current air quality by station with the European Air Quality Index level of each pollutant and of the station
 * (its worst pollutant). Serves stored data only; an empty list means nothing has been imported yet.
 */
@RestController
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class AirQualityController {
    private final AirQualityStore store;

    @GetMapping("/api/v1/air-quality")
    ResponseEntity<Response> airQuality() {
        var snapshot = store.read();
        var stations = snapshot.map(stored -> stored.stations().stream().map(AirQualityController::station).toList())
                .orElse(List.of());
        var source = new Source(SOURCE_NAME, CATALOG_URL, LICENSE, LICENSE_URL, INDEX_NAME, INDEX_URL,
                snapshot.map(AirQualitySnapshot::retrievedAt).orElse(null));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Response(source, stations));
    }

    private static Station station(AirQualitySnapshot.Station stored) {
        var readings = Arrays.stream(Pollutant.values())
                .filter(pollutant -> stored.readings().containsKey(pollutant.key()))
                .map(pollutant -> {
                    var value = stored.readings().get(pollutant.key());
                    return new Reading(pollutant.key(), value, pollutant.level(value).map(AirQualityLevel::key).orElse(null));
                }).toList();
        var level = Arrays.stream(Pollutant.values())
                .filter(pollutant -> stored.readings().containsKey(pollutant.key()))
                .flatMap(pollutant -> pollutant.level(stored.readings().get(pollutant.key())).stream())
                .max(Comparator.naturalOrder()).map(AirQualityLevel::key).orElse(null);
        return new Station(stored.id(), Map.of("en", stored.nameEn(), "el", stored.nameEl().isEmpty() ? stored.nameEn() : stored.nameEl()),
                stored.type(), stored.latitude(), stored.longitude(), stored.measuredAt(), level, readings);
    }

    record Response(Source source, List<Station> stations) {}
    record Source(String name, String url, String license, String licenseUrl, String index, String indexUrl,
                  Instant retrievedAt) {}
    record Station(String id, Map<String, String> name, String type, double latitude, double longitude,
                   Instant measuredAt, String level, List<Reading> readings) {}
    record Reading(String pollutant, double value, String level) {}
}
