package com.cyprusopen.places.recycling;

import java.time.Instant;
import java.util.List;

/**
 * Green Dot glass banks and recycling islands.
 *
 * @param retrievedAt when the file was last downloaded
 * @param publishedAt when Green Dot exported the points (the date in the file name)
 */
record RecyclingSnapshot(Instant retrievedAt, Instant publishedAt, List<Point> points) {
    /**
     * @param kind        "glass" (glass bank) or "island" (packaging, paper and glass bins together)
     * @param code        Green Dot's own label of the point ("K107"), not unique in the file
     * @param description where the point is (Greek, as published: "Πάρκο"), null when missing
     */
    record Point(String id, String kind, String code, String description, double latitude, double longitude) {}
}
