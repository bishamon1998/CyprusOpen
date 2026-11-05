package com.cyprusopen.statistics;

import java.util.regex.Pattern;

/** One stored value: a series key, an ISO-like period ({@code 2024} or {@code 2024-07}) and the source value. */
record Observation(String series, String period, double value, boolean lowReliability) {
    private static final Pattern YEAR = Pattern.compile("\\d{4}");
    private static final Pattern MONTH = Pattern.compile("(\\d{4})M(0[1-9]|1[0-2])");

    /** Converts CYSTAT period labels ({@code 2024}, {@code 2024M07}) to {@code 2024} or {@code 2024-07}. */
    static String period(String cystat) {
        if (YEAR.matcher(cystat).matches()) return cystat;
        var month = MONTH.matcher(cystat);
        if (month.matches()) return month.group(1) + "-" + month.group(2);
        throw new IllegalArgumentException("Unsupported period " + cystat);
    }
}
