package com.cyprusopen.parking.beach;

import java.time.Duration;

final class BeachConstants {
    static final int SNAPSHOT_ID = 1;
    static final int MAX_RESPONSE_BYTES = 8 * 1024 * 1024;
    static final int MAX_BEACHES = 1000;
    /** Seasons of classification history kept per beach, newest first. */
    static final int HISTORY_SEASONS = 10;
    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    static final Duration READ_TIMEOUT = Duration.ofSeconds(60);
    /** EEA publishes one season a year; a start-up refresh runs when the last import is older than this. */
    static final Duration REFRESH_AFTER = Duration.ofDays(7);
    static final String COUNTRY = "CY";
    /** EEA Discodata tables of the Bathing Water Directive reporting, latest version. */
    static final String SPATIAL = "spatial_ProtectedArea";
    static final String STATUS = "assessment_BathingWaterStatus";
    static final String QUALITY = "assessment_BathingWaterQuality";
    static final String SOURCE_NAME = "European Environment Agency (EEA), reported by the Department of Environment";
    static final String SOURCE_URL = "https://www.eea.europa.eu/en/topics/in-depth/bathing-water";
    static final String LICENSE = "CC BY 4.0";
    static final String LICENSE_URL = "https://www.eea.europa.eu/en/legal-notice";

    private BeachConstants() {}
}
