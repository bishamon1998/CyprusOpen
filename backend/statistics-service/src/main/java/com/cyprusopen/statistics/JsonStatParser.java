package com.cyprusopen.statistics;

import static com.cyprusopen.statistics.StatisticsConstants.JSON_STAT_VERSION;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Parses JSON-stat 2.0 responses; any structural mismatch is reported as {@link IllegalArgumentException}. */
@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class JsonStatParser {
    private final JsonMapper mapper;

    JsonStat parse(byte[] body) {
        JsonNode root;
        try {
            root = mapper.readTree(body);
        } catch (JacksonException invalid) {
            throw new IllegalArgumentException("Response is not JSON", invalid);
        }
        if (!JSON_STAT_VERSION.equals(root.path("version").asString(""))
                || !"dataset".equals(root.path("class").asString(""))) {
            throw new IllegalArgumentException("Not a JSON-stat 2.0 dataset");
        }
        var ids = root.path("id");
        var sizes = root.path("size");
        if (!ids.isArray() || ids.isEmpty() || ids.size() != sizes.size()) {
            throw new IllegalArgumentException("Invalid dimensions");
        }
        var dimensions = new ArrayList<JsonStat.Dimension>();
        var cells = 1;
        for (var index = 0; index < ids.size(); index++) {
            var id = ids.get(index).asString();
            var labels = labels(root.path("dimension").path(id).path("category"));
            if (labels.size() != sizes.get(index).asInt()) throw new IllegalArgumentException("Size mismatch: " + id);
            dimensions.add(new JsonStat.Dimension(id, labels));
            cells = Math.multiplyExact(cells, labels.size());
        }
        return new JsonStat(updated(root), List.copyOf(dimensions), values(root.path("value"), cells),
                status(root.path("status"), cells));
    }

    private static Instant updated(JsonNode root) {
        try {
            return Instant.parse(root.path("updated").asString(""));
        } catch (DateTimeParseException invalid) {
            throw new IllegalArgumentException("Missing update time", invalid);
        }
    }

    private static List<String> labels(JsonNode category) {
        var index = category.path("index");
        var codes = new ArrayList<String>();
        if (index.isArray()) {
            index.forEach(code -> codes.add(code.asString()));
        } else if (index.isObject()) {
            var ordered = new String[index.size()];
            for (var entry : index.properties()) {
                var position = entry.getValue().asInt(-1);
                if (position < 0 || position >= ordered.length || ordered[position] != null) {
                    throw new IllegalArgumentException("Invalid category index");
                }
                ordered[position] = entry.getKey();
            }
            codes.addAll(List.of(ordered));
        } else {
            throw new IllegalArgumentException("Missing category index");
        }
        var labels = category.path("label");
        return codes.stream().map(code -> labels.path(code).asString(code).strip()).toList();
    }

    private static List<Double> values(JsonNode value, int cells) {
        var values = new ArrayList<Double>(cells);
        if (value.isArray()) {
            if (value.size() != cells) throw new IllegalArgumentException("Value count mismatch");
            value.forEach(cell -> values.add(number(cell)));
        } else if (value.isObject()) {
            for (var index = 0; index < cells; index++) values.add(null);
            for (var entry : value.properties()) values.set(position(entry.getKey(), cells), number(entry.getValue()));
        } else {
            throw new IllegalArgumentException("Missing values");
        }
        return values;
    }

    private static Map<Integer, String> status(JsonNode status, int cells) {
        var statuses = new HashMap<Integer, String>();
        if (status.isString()) {
            for (var index = 0; index < cells; index++) statuses.put(index, status.asString());
        } else if (status.isArray()) {
            for (var index = 0; index < status.size(); index++) {
                if (!status.get(index).isNull()) statuses.put(index, status.get(index).asString());
            }
        } else if (status.isObject()) {
            for (var entry : status.properties()) {
                statuses.put(position(entry.getKey(), cells), entry.getValue().asString());
            }
        }
        return statuses;
    }

    private static Double number(JsonNode cell) {
        if (cell.isNull()) return null;
        if (!cell.isNumber() || !Double.isFinite(cell.asDouble())) throw new IllegalArgumentException("Non-numeric value");
        return cell.asDouble();
    }

    private static int position(String key, int cells) {
        int position;
        try {
            position = Integer.parseInt(key);
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException("Invalid cell position " + key, invalid);
        }
        if (position < 0 || position >= cells) throw new IllegalArgumentException("Invalid cell position " + key);
        return position;
    }
}
