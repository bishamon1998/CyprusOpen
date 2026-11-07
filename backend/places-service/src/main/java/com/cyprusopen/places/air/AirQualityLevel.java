package com.cyprusopen.places.air;

import java.util.Locale;

/** European Air Quality Index levels, from best to worst. */
enum AirQualityLevel {
    GOOD, FAIR, MODERATE, POOR, VERY_POOR, EXTREMELY_POOR;

    /** API key: "good", "very-poor", … (Locale.ROOT: in a Turkish default locale "FAIR" would become "faır"). */
    String key() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
