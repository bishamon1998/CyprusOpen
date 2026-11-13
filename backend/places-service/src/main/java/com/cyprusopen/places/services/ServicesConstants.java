package com.cyprusopen.places.services;

import java.time.Duration;

final class ServicesConstants {
    /** Both files are a few kilobytes. */
    static final int MAX_FILE_BYTES = 512 * 1024;
    static final int MAX_PLACES = 500;
    /** The files change a few times a year at most; a start-up refresh runs when the last import is older than this. */
    static final Duration REFRESH_AFTER = Duration.ofDays(30);

    /** data.gov.cy file names, URL-encoded, relative to the configured {@code services.files-url}. */
    // "Κατάλογος Νοσοκομείων Κύπρου_1.csv"
    static final String HOSPITALS_FILE = "%CE%9A%CE%B1%CF%84%CE%AC%CE%BB%CE%BF%CE%B3%CE%BF%CF%82%20"
            + "%CE%9D%CE%BF%CF%83%CE%BF%CE%BA%CE%BF%CE%BC%CE%B5%CE%AF%CF%89%CE%BD%20%CE%9A%CF%8D%CF%80%CF%81%CE%BF%CF%85_1.csv";
    static final String CENTRES_FILE = "CSC%20Info%20and%20Location%20Data_5.csv";

    static final String HOSPITALS_SOURCE_NAME = "Ministry of Health";
    static final String HOSPITALS_SOURCE_URL =
            "https://www.data.gov.cy/el/dataset/i/2e27aa7a-75de-485d-8b07-d2dfc734f8ab";
    static final String CENTRES_SOURCE_NAME = "Public Administration and Personnel Department";
    static final String CENTRES_SOURCE_URL =
            "https://www.data.gov.cy/el/dataset/i/6b8d7db3-b5ff-4dd0-8df0-d4cb60b0d26a";
    static final String LICENSE = "CC BY 4.0";
    static final String LICENSE_URL = "https://creativecommons.org/licenses/by/4.0/";

    private ServicesConstants() {}
}
