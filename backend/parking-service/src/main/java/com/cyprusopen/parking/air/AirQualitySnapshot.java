package com.cyprusopen.parking.air;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** The latest hourly readings of every station, stored as one document. */
record AirQualitySnapshot(Instant retrievedAt, List<Station> stations) {
    /** {@code readings} maps a {@link Pollutant} key to µg/m³. */
    record Station(String id, String nameEn, String nameEl, String type, double latitude, double longitude,
                   Instant measuredAt, Map<String, Double> readings) {}
}
