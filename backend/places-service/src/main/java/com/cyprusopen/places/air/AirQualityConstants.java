package com.cyprusopen.places.air;

import java.time.Duration;
import java.time.ZoneId;

final class AirQualityConstants {
    static final int MAX_RESPONSE_BYTES = 1024 * 1024;
    static final int MAX_STATIONS = 200;
    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    static final Duration READ_TIMEOUT = Duration.ofSeconds(15);
    /** The source updates hourly; a start-up refresh runs when the last check is older than this. */
    static final Duration REFRESH_AFTER = Duration.ofMinutes(50);
    /** The source publishes measurement times in Cyprus local time without an offset. */
    static final ZoneId SOURCE_ZONE = ZoneId.of("Asia/Nicosia");
    static final String SOURCE_NAME = "Department of Labour Inspection";
    static final String CATALOG_URL = "https://www.data.gov.cy/el/dataset/i/5dc97c12-735d-4cc7-9030-101c735ce1a3";
    static final String LICENSE = "CC BY-SA 4.0";
    static final String LICENSE_URL = "https://creativecommons.org/licenses/by-sa/4.0/";
    static final String INDEX_NAME = "European Air Quality Index (EEA)";
    static final String INDEX_URL = "https://airindex.eea.europa.eu/AQI/index.html";

    private AirQualityConstants() {}
}
