package com.cyprusopen.parking.air;

/** European Air Quality Index levels, from best to worst. */
enum AirQualityLevel {
    GOOD, FAIR, MODERATE, POOR, VERY_POOR, EXTREMELY_POOR;

    /** API key: "good", "very-poor", … */
    String key() {
        return name().toLowerCase().replace('_', '-');
    }
}
