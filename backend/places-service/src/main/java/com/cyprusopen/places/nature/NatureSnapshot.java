package com.cyprusopen.places.nature;

import java.time.Instant;
import java.util.List;

/**
 * Picnic sites, camping sites and nature trails of the Department of Forests.
 *
 * @param retrievedAt when the files were last downloaded
 * @param publishedAt when the source last changed them (Last-Modified), null when unknown
 */
record NatureSnapshot(Instant retrievedAt, Instant publishedAt, List<Site> sites) {
    /**
     * @param kind       "picnic", "camping" or "trail"
     * @param name       name in Latin letters, as published
     * @param greekName  Greek name, null when the source has none
     * @param latitude   site position; for a trail, the first point of its first line (not always the trailhead)
     * @param lengthKm   trail length from the source geometry, null for other kinds
     * @param capacity   maximum number of people (camping sites), null when not published
     * @param route      trail lines as [latitude, longitude] pairs, empty for other kinds
     */
    record Site(String id, String kind, String name, String greekName, double latitude, double longitude,
                Double lengthKm, Integer capacity, List<List<double[]>> route) {}
}
