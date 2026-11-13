package com.cyprusopen.places.recycling;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;

final class RecyclingConstants {
    /** The file is about 120 KB. */
    static final int MAX_FILE_BYTES = 2 * 1024 * 1024;
    static final int MAX_POINTS = 5000;
    /** The file has not changed since 2019; a start-up refresh runs when the last import is older than this. */
    static final Duration REFRESH_AFTER = Duration.ofDays(30);

    /** The data.gov.cy file name, relative to the configured {@code recycling.files-url}; dated 24 June 2019. */
    static final String POINTS_FILE = "db-greendot-pois_190624_toOpenData.csv";
    /**
     * The export date in the file name. The file's Last-Modified (23 October 2023) is when data.gov.cy moved to a new
     * platform, so it would make the points look four years newer than they are.
     */
    static final LocalDate EXPORTED_ON = LocalDate.of(2019, 6, 24);
    static final ZoneId CYPRUS = ZoneId.of("Asia/Nicosia");

    static final String SOURCE_NAME = "Green Dot Cyprus";
    static final String SOURCE_URL = "https://www.data.gov.cy/el/dataset/i/b9e4d56b-0fee-4453-8f03-84210265731d";
    static final String LICENSE = "CC BY-SA 4.0";
    static final String LICENSE_URL = "https://creativecommons.org/licenses/by-sa/4.0/";

    private RecyclingConstants() {}
}
