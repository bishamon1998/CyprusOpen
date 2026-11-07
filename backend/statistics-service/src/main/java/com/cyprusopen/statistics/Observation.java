package com.cyprusopen.statistics;

import java.util.regex.Pattern;

/**
 * One stored value: a series key, an ISO-like period ({@code 2024}, {@code 2024-07} or {@code 2024-Q3})
 * and the source value.
 */
record Observation(String series, String period, double value, boolean lowReliability) {
    private static final Pattern YEAR = Pattern.compile("\\d{4}");
    private static final Pattern MONTH = Pattern.compile("(\\d{4})M(0[1-9]|1[0-2])");
    private static final Pattern QUARTER = Pattern.compile("(\\d{4})Q([1-4])");
    /** Revision and provisional markers CYSTAT appends to a period, such as "2023 (r)". */
    private static final Pattern NOTE = Pattern.compile("\\s*\\([a-z]+\\)$");

    /** Converts CYSTAT period labels ({@code 2024}, {@code 2024M07}, {@code 2024Q3}) to ISO-like periods. */
    static String period(String cystat) {
        var label = NOTE.matcher(cystat).replaceFirst("");
        if (YEAR.matcher(label).matches()) return label;
        var month = MONTH.matcher(label);
        if (month.matches()) return month.group(1) + "-" + month.group(2);
        var quarter = QUARTER.matcher(label);
        if (quarter.matches()) return quarter.group(1) + "-Q" + quarter.group(2);
        throw new IllegalArgumentException("Unsupported period " + cystat);
    }
}
