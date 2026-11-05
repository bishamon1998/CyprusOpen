package com.cyprusopen.pharmacies;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;

final class PharmacyScheduleConstants {
    static final ZoneId CYPRUS = ZoneId.of("Asia/Nicosia");
    static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("d/M/uu")
            .withResolverStyle(ResolverStyle.STRICT);
    static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("d/M/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    static final List<String> DISTRICTS = List.of("nicosia", "larnaca", "limassol", "paphos", "famagusta");
    static final String SOURCE = "https://www.data.gov.cy/el/dataset/katalogos-diimereyonton-farmakeion";

    private PharmacyScheduleConstants() {}
}
