package com.cyprusopen.parking.air;

import java.util.Arrays;
import java.util.Optional;

/**
 * Pollutants kept from the source, by its numeric code (the EIONET air pollutant vocabulary id). The upper
 * bounds of the European Air Quality Index levels Good … Very poor are in µg/m³, from the EEA index page; CO has
 * no index level.
 */
enum Pollutant {
    PM25("6001", 5, 15, 50, 90, 140),
    PM10("5", 15, 45, 120, 195, 270),
    O3("7", 60, 100, 120, 160, 180),
    NO2("8", 10, 25, 60, 100, 150),
    SO2("1", 20, 40, 125, 190, 275),
    CO("10");

    private final String code;
    private final int[] bounds;

    Pollutant(String code, int... bounds) {
        this.code = code;
        this.bounds = bounds;
    }

    static Optional<Pollutant> byCode(String code) {
        return Arrays.stream(values()).filter(pollutant -> pollutant.code.equals(code)).findFirst();
    }

    /** API key: "pm25", "no2", … */
    String key() {
        return name().toLowerCase();
    }

    /** Index level of an hourly concentration; the bands are whole µg/m³ ("6-15"), so the value is rounded. */
    Optional<AirQualityLevel> level(double value) {
        if (bounds.length == 0) return Optional.empty();
        var rounded = Math.round(value);
        for (var index = 0; index < bounds.length; index++) {
            if (rounded <= bounds[index]) return Optional.of(AirQualityLevel.values()[index]);
        }
        return Optional.of(AirQualityLevel.EXTREMELY_POOR);
    }
}
