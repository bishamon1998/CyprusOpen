package com.cyprusopen.parking;

import java.time.Duration;
import java.util.Set;

final class DirectoryConstants {
    static final Set<String> HTTP_SCHEMES = Set.of("http", "https");
    static final int DIRECTORY_ID = 1;
    static final int MAX_RESPONSE_BYTES = 2 * 1024 * 1024;
    static final int MAX_LOCATIONS = 20_000;
    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    static final Duration READ_TIMEOUT = Duration.ofSeconds(15);
    static final Duration MAX_CLOCK_SKEW = Duration.ofMinutes(5);
    static final double MIN_LATITUDE = 34;
    static final double MAX_LATITUDE = 36;
    static final double MIN_LONGITUDE = 32;
    static final double MAX_LONGITUDE = 35;
    static final String MONTHLY_CRON = "0 0 3 1 * *";
    static final String SCHEDULE_ZONE = "UTC";
    static final String USER_AGENT = "CyprusOpen/0.1 (+https://github.com/bishamon1998/CyprusOpen)";
    static final String SOURCE_NAME = "Cyprus National Access Point / Public Works Department";
    static final String CATALOG_URL = "https://www.traffic4cyprus.org.cy/en/dataset/parking_areas";
    static final String RESOURCE_URL = "https://fixcyprus.cy/gnosis/open/api/nap/datasets/parking_areas/";
    static final String LICENSE = "CC BY 4.0";
    static final String LICENSE_URL = "https://creativecommons.org/licenses/by/4.0/";
    static final String TRANSFORMATIONS = "English names only; missing/non-English names and OSM Origin placeholders are unnamed. Coordinates and identifiers preserved.";
    static final String PARKING_NAMESPACE = "http://datex2.eu/schema/3/parking";
    static final String COMMON_NAMESPACE = "http://datex2.eu/schema/3/common";
    static final String ENGLISH_TEXT_PATTERN = "[\\x20-\\x7E]+";
    static final String UNNAMED_SOURCE_LABEL = "OSM Origin";
    static final String LOCATION_ID_PREFIX = "cynap-";
    static final String UNNAMED_LOCATION = "Car park";
    static final String UPDATED = "updated";
    static final String UNCHANGED = "unchanged";
    static final String UNAVAILABLE = "unavailable";
    static final String INVALID = "invalid";

    private DirectoryConstants() {}
}
