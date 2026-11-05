package com.cyprusopen.statistics;

import static com.cyprusopen.statistics.StatisticsConstants.JSON_STAT_VERSION;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Binds JSON-stat 2.0 responses with Jackson and orders each dimension's categories by their index.
 * Covers the form PxWeb returns (object category index, dense value array, sparse status object);
 * anything else is rejected as {@link IllegalArgumentException}.
 */
@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class JsonStatParser {
    private final JsonMapper mapper;

    JsonStat parse(byte[] body) {
        Document document;
        try {
            document = mapper.readValue(body, Document.class);
        } catch (JacksonException invalid) {
            throw new IllegalArgumentException("Not a JSON-stat document", invalid);
        }
        if (!JSON_STAT_VERSION.equals(document.version()) || !"dataset".equals(document.type())
                || document.updated() == null || document.id() == null || document.size() == null
                || document.value() == null || document.id().size() != document.size().size()) {
            throw new IllegalArgumentException("Not a JSON-stat 2.0 dataset");
        }
        var dimensions = new ArrayList<JsonStat.Dimension>();
        var cells = 1;
        for (var index = 0; index < document.id().size(); index++) {
            var id = document.id().get(index);
            var dimension = document.dimension() == null ? null : document.dimension().get(id);
            if (dimension == null || dimension.category() == null) throw new IllegalArgumentException("Missing " + id);
            var labels = dimension.category().labels();
            if (labels.size() != document.size().get(index)) throw new IllegalArgumentException("Size mismatch: " + id);
            dimensions.add(new JsonStat.Dimension(id, labels));
            cells = Math.multiplyExact(cells, labels.size());
        }
        if (document.value().size() != cells) throw new IllegalArgumentException("Value count mismatch");
        var status = document.status() == null ? Map.<Integer, String>of() : document.status();
        return new JsonStat(document.updated(), List.copyOf(dimensions), document.value(), status);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Document(String version, @JsonProperty("class") String type, Instant updated, List<String> id,
                    List<Integer> size, Map<String, Dimension> dimension, List<Double> value,
                    Map<Integer, String> status) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Dimension(Category category) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Category(Map<String, Integer> index, Map<String, String> label) {
        /** Category labels in index order; a category without a label uses its code. */
        List<String> labels() {
            if (index == null) throw new IllegalArgumentException("Missing category index");
            var ordered = new String[index.size()];
            index.forEach((code, position) -> {
                if (position == null || position < 0 || position >= ordered.length || ordered[position] != null) {
                    throw new IllegalArgumentException("Invalid category index");
                }
                ordered[position] = label == null ? code : label.getOrDefault(code, code).strip();
            });
            return List.of(ordered);
        }
    }
}
