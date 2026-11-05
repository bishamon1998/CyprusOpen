package com.cyprusopen.parking;

import java.time.Duration;

final class DirectoryConstants {
    static final int DIRECTORY_ID = 1;
    static final int MAX_RESPONSE_BYTES = 2 * 1024 * 1024;
    static final int MAX_LOCATIONS = 20_000;
    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    static final Duration READ_TIMEOUT = Duration.ofSeconds(15);
    static final Duration MAX_CLOCK_SKEW = Duration.ofMinutes(5);
    static final String MONTHLY_CRON = "0 0 3 1 * *";
    static final String SCHEDULE_ZONE = "UTC";
    static final String SOURCE_NAME = "Cyprus National Access Point / Public Works Department";
    static final String CATALOG_URL = "https://www.traffic4cyprus.org.cy/en/dataset/parking_areas";
    static final String RESOURCE_URL = "https://fixcyprus.cy/gnosis/open/api/nap/datasets/parking_areas/";
    static final String LICENSE = "CC BY 4.0";
    static final String LICENSE_URL = "https://creativecommons.org/licenses/by/4.0/";
    static final String TRANSFORMATIONS = "English names only; missing/non-English names and OSM Origin placeholders are unnamed. Coordinates and identifiers preserved.";
    static final String PUBLICATION_ELEMENT = "ParkingFacilitiesPublication";
    static final String PARKING_NAMESPACE = "http://datex2.eu/schema/3/parking";
    static final String COMMON_NAMESPACE = "http://datex2.eu/schema/3/common";
    static final String ENGLISH_LANGUAGE = "en";
    static final String ENGLISH_TEXT_PATTERN = "[\\x20-\\x7E]+";
    static final String UNNAMED_SOURCE_LABEL = "OSM Origin";
    static final String LOCATION_ID_PREFIX = "cynap-";
    static final String UNNAMED_LOCATION = "Car park";
    /** Live occupancy is not integrated yet, so the API always reports it as unavailable. */
    static final String FRESHNESS_UNAVAILABLE = "unavailable";

    private DirectoryConstants() {}
}
