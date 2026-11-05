package com.cyprusopen.common.geo;

/** Generous bounding box around Cyprus used to reject obviously misplaced source coordinates. */
public final class CyprusBounds {
    public static final double MIN_LATITUDE = 34;
    public static final double MAX_LATITUDE = 36;
    public static final double MIN_LONGITUDE = 32;
    public static final double MAX_LONGITUDE = 35;

    private CyprusBounds() {}

    public static boolean contains(double latitude, double longitude) {
        return Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude >= MIN_LATITUDE && latitude <= MAX_LATITUDE
                && longitude >= MIN_LONGITUDE && longitude <= MAX_LONGITUDE;
    }
}
