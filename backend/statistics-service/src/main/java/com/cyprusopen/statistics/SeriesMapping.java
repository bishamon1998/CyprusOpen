package com.cyprusopen.statistics;

import static com.cyprusopen.statistics.StatisticsConstants.LOW_RELIABILITY_STATUS;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Turns a CYSTAT table into series observations. The time dimension becomes periods, zero or more
 * dimensions become series keys, and every other dimension must be pinned to one category (usually a total).
 */
final class SeriesMapping {
    private SeriesMapping() {}

    /** One series dimension whose category labels map to keys; unmapped categories are skipped. */
    static List<Observation> map(JsonStat table, String series, Function<String, Optional<String>> key,
                                 String period, Map<String, String> pinned) {
        return map(table, period, pinned, List.of(series), labels -> key.apply(labels.getFirst()));
    }

    /** A table holding a single series, such as the annual inflation rate. */
    static List<Observation> single(JsonStat table, String period, Map<String, String> pinned, String key) {
        return map(table, period, pinned, List.of(), labels -> Optional.of(key));
    }

    /**
     * @param period  time dimension
     * @param pinned  category label required for each dimension that is neither time nor series
     * @param series  dimensions whose category labels together form the series key
     * @param key     maps the series categories (in {@code series} order) to a key; empty skips the cell
     */
    static List<Observation> map(JsonStat table, String period, Map<String, String> pinned, List<String> series,
                                 Function<List<String>, Optional<String>> key) {
        var periodIndex = table.dimension(period);
        var seriesIndexes = series.stream().map(table::dimension).toList();
        var pins = new LinkedHashMap<Integer, String>();
        pinned.forEach((dimension, label) -> pins.put(table.dimension(dimension), label));
        if (pins.size() + seriesIndexes.size() + 1 != table.dimensions().size()) {
            throw new IllegalArgumentException("Unpinned dimension");
        }
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
            var seriesKey = key.apply(seriesIndexes.stream().map(cell::label).toList());
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
