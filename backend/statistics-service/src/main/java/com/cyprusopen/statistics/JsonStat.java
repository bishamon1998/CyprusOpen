package com.cyprusopen.statistics;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** A decoded JSON-stat 2.0 dataset: ordered dimensions with category labels and a dense value array. */
record JsonStat(Instant updated, List<Dimension> dimensions, List<Double> values, Map<Integer, String> status) {
    record Dimension(String id, List<String> labels) {}

    /** One table cell with the category label of every dimension, in dimension order. */
    record Cell(List<String> labels, Double value, String status) {
        String label(int dimension) {
            return labels.get(dimension);
        }
    }

    int dimension(String id) {
        for (var index = 0; index < dimensions.size(); index++) {
            if (dimensions.get(index).id().equals(id)) return index;
        }
        throw new IllegalArgumentException("Missing dimension " + id);
    }

    List<Cell> cells() {
        var cells = new ArrayList<Cell>(values.size());
        var position = new int[dimensions.size()];
        for (var index = 0; index < values.size(); index++) {
            var labels = new ArrayList<String>(dimensions.size());
            for (var dimension = 0; dimension < dimensions.size(); dimension++) {
                labels.add(dimensions.get(dimension).labels().get(position[dimension]));
            }
            cells.add(new Cell(List.copyOf(labels), values.get(index), status.get(index)));
            for (var dimension = dimensions.size() - 1; dimension >= 0; dimension--) {
                if (++position[dimension] < dimensions.get(dimension).labels().size()) break;
                position[dimension] = 0;
            }
        }
        return cells;
    }
}
