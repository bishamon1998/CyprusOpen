package com.cyprusopen.pharmacies;

import java.time.Duration;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;

final class PharmacyConstants {
    static final ZoneId CYPRUS = ZoneId.of("Asia/Nicosia");
    static final String SOURCE = "https://www.data.gov.cy/el/dataset/katalogos-diimereyonton-farmakeion";

    // Upstream access
    static final int MAX_RESPONSE_BYTES = 2 * 1024 * 1024;
    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

    static final Duration MAX_SCHEDULE_AGE = Duration.ofDays(1);

    // DCAT-AP catalog metadata fields
    static final String DISTRIBUTIONS = "dcat:Distribution";
    static final String DISTRIBUTION_TITLE = "dct:title";
    static final String DOWNLOAD_URL = "dcat:downloadURL";
    static final String RESOURCE = "@rdf:resource";

    // District CSV columns, spelled exactly as the source publishes them
    static final String BYTE_ORDER_MARK = "﻿";
    static final String DATE_COLUMN = "Date";
    static final String REGISTRATION_COLUMN = "Reg. No.";
    static final String FIRST_NAME_COLUMN = "Name";
    static final String SURNAME_COLUMN = "Surmame";
    static final String ADDRESS_COLUMN = "Address";
    static final String MUNICIPALITY_COLUMN = "Muniuciplity / Community";
    static final String PHONE_COLUMN = "Pharmacy Tel. No.";
    static final List<String> REQUIRED_COLUMNS = List.of(REGISTRATION_COLUMN, FIRST_NAME_COLUMN, SURNAME_COLUMN,
            ADDRESS_COLUMN, MUNICIPALITY_COLUMN, PHONE_COLUMN);
    static final String REGISTRATION_PATTERN = "[0-9]+";
    static final String SHORT_YEAR_PATTERN = ".*/[0-9]{2}";
    static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("d/M/uu")
            .withResolverStyle(ResolverStyle.STRICT);
    static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("d/M/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    // Duty periods: the roster changes at 08:00; late evening and overnight service is on call
    static final LocalTime ROSTER_CHANGE = LocalTime.of(8, 0);
    static final LocalTime WINTER_ON_CALL_START = LocalTime.of(22, 0);
    static final LocalTime SUMMER_ON_CALL_START = LocalTime.of(23, 0);
    static final MonthDay SUMMER_START = MonthDay.of(5, 1);
    static final MonthDay SUMMER_END = MonthDay.of(9, 30);

    private PharmacyConstants() {}
}
