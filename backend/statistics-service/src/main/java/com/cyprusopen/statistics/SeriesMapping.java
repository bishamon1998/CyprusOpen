package com.cyprusopen.statistics;

import static com.cyprusopen.statistics.StatisticsConstants.LOW_RELIABILITY_STATUS;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Turns a CYSTAT table into series observations: one dimension becomes series keys, one becomes periods,
 * and every other dimension must be pinned to a single category (usually its total).
 */
final class SeriesMapping {
    private SeriesMapping() {}

    /**
     * @param series    dimension whose category labels become series keys
     * @param key       maps a category label to a series key; empty skips the category
     * @param period    time dimension
     * @param pinned    category label required for each remaining dimension
     */
    static List<Observation> map(JsonStat table, String series, Function<String, Optional<String>> key,
                                 String period, Map<String, String> pinned) {
        var seriesIndex = table.dimension(series);
        var periodIndex = table.dimension(period);
        var pins = new LinkedHashMap<Integer, String>();
        pinned.forEach((dimension, label) -> pins.put(table.dimension(dimension), label));
        if (pins.size() + 2 != table.dimensions().size()) throw new IllegalArgumentException("Unpinned dimension");
        pins.forEach((dimension, label) -> {
            if (!table.dimensions().get(dimension).labels().contains(label)) {
                throw new IllegalArgumentException("Missing category " + label);
            }
        });

        // Categories that map to the same key (a country listed under two regions over time) are added up.
        var merged = new LinkedHashMap<String, Observation>();
        for (var cell : table.cells()) {
            if (cell.value() == null) continue;
            if (pins.entrySet().stream().anyMatch(pin -> !cell.label(pin.getKey()).equals(pin.getValue()))) continue;
            var seriesKey = key.apply(cell.label(seriesIndex));
            if (seriesKey.isEmpty()) continue;
            var observation = new Observation(seriesKey.get(), Observation.period(cell.label(periodIndex)),
                    cell.value(), LOW_RELIABILITY_STATUS.equals(cell.status()));
            merged.merge(observation.series() + "|" + observation.period(), observation, (first, second) ->
                    new Observation(first.series(), first.period(), first.value() + second.value(),
                            first.lowReliability() || second.lowReliability()));
        }
        return new ArrayList<>(merged.values());
    }

    /** Series key lookup for tables whose categories are a fixed list of labels. */
    static Function<String, Optional<String>> labels(Map<String, String> keys) {
        return label -> Optional.ofNullable(keys.get(label));
    }
}
