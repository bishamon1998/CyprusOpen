package com.cyprusopen.pharmacies;

/**
 * Stored map point for a pharmacy. Precision is {@code exact}, {@code street} or {@code locality};
 * source names where the point came from.
 */
record PharmacyLocation(double latitude, double longitude, String precision, String source) {}
