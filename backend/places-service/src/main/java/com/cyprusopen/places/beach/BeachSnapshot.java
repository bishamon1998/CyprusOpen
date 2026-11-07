package com.cyprusopen.places.beach;

import java.time.Instant;
import java.util.List;

/** Every currently designated bathing water with its classification history and latest sample statistics. */
record BeachSnapshot(Instant retrievedAt, Integer season, List<Beach> beaches) {
    /**
     * @param quality      classification of the latest season ("excellent", "good", "poor", "not-classified", …)
     * @param history      classification by season, newest first
     * @param samples      samples taken in the latest assessed season
     * @param escherichiaColi       95th percentile of E. coli, cfu/100 ml
     * @param intestinalEnterococci 95th percentile of intestinal enterococci, cfu/100 ml
     */
    record Beach(String id, String name, double latitude, double longitude, String profileUrl, String quality,
                 Integer season, List<Classification> history, Integer samples, Double escherichiaColi,
                 Double intestinalEnterococci) {}

    record Classification(int season, String quality) {}
}
