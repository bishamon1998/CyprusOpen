package com.cyprusopen.statistics;

import java.time.Duration;

final class StatisticsConstants {
    // Upstream access
    static final int MAX_RESPONSE_BYTES = 4 * 1024 * 1024;
    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    static final Duration READ_TIMEOUT = Duration.ofSeconds(30);
    /** PxWeb query selecting every cell of a table, returned as JSON-stat 2.0. */
    static final String FULL_TABLE_QUERY = "{\"query\":[],\"response\":{\"format\":\"json-stat2\"}}";

    // Attribution
    static final String SOURCE_NAME = "Statistical Service of Cyprus (CYSTAT)";
    static final String LICENSE = "CC BY 4.0";
    static final String LICENSE_URL = "https://creativecommons.org/licenses/by/4.0/";
    static final String DATASET_PAGE = "https://www.data.gov.cy/en/dataset/";

    // JSON-stat
    static final String JSON_STAT_VERSION = "2.0";
    static final String LOW_RELIABILITY_STATUS = "u";
    static final String TOTAL_SERIES = "total";

    private StatisticsConstants() {}
}
