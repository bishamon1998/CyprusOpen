package com.cyprusopen.places.air;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class PollutantTest {
    @Test
    void mapsConcentrationsToIndexLevelsAtTheBandEdges() {
        assertThat(Pollutant.PM25.level(5)).contains(AirQualityLevel.GOOD);
        // Bands are whole µg/m³: 5.4 rounds to 5 (good), 5.5 to 6 (fair).
        assertThat(Pollutant.PM25.level(5.4)).contains(AirQualityLevel.GOOD);
        assertThat(Pollutant.PM25.level(5.5)).contains(AirQualityLevel.FAIR);
        assertThat(Pollutant.PM10.level(270)).contains(AirQualityLevel.VERY_POOR);
        assertThat(Pollutant.PM10.level(271)).contains(AirQualityLevel.EXTREMELY_POOR);
        assertThat(Pollutant.CO.level(1_000)).isEmpty();
        assertThat(Pollutant.byCode("6001")).contains(Pollutant.PM25);
        assertThat(Pollutant.byCode("999")).isEmpty();
    }

    @Test
    void apiKeysDoNotDependOnTheDefaultLocale() {
        var original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));
            assertThat(AirQualityLevel.FAIR.key()).isEqualTo("fair");
            assertThat(AirQualityLevel.EXTREMELY_POOR.key()).isEqualTo("extremely-poor");
            assertThat(Pollutant.PM25.key()).isEqualTo("pm25");
        } finally {
            Locale.setDefault(original);
        }
    }
}
