package com.cyprusopen.statistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** Upstream JSON-stat is untrusted: every unexpected shape must be an {@link IllegalArgumentException}. */
class JsonStatParserTest {
    private static final String VALID = """
            {"version":"2.0","class":"dataset","updated":"2026-10-19T05:00:00Z",
             "id":["YEAR","SEX"],"size":[2,2],
             "dimension":{
               "YEAR":{"category":{"index":{"2025":1,"2024":0}}},
               "SEX":{"category":{"index":{"T":0,"F":1},"label":{"T":" Total ","F":null}}}},
             "value":[1,2,3,null],"status":{"1":"u"}}
            """;

    private final JsonStatParser parser = new JsonStatParser(JsonMapper.builder().build());

    @Test
    void ordersCategoriesByIndexAndFallsBackToCodesForMissingLabels() {
        var table = parser.parse(bytes(VALID));
        assertThat(table.updated()).isEqualTo(Instant.parse("2026-10-19T05:00:00Z"));
        assertThat(table.dimensions().get(0).labels()).containsExactly("2024", "2025");
        assertThat(table.dimensions().get(1).labels()).containsExactly("Total", "F");
        var cells = table.cells();
        assertThat(cells).hasSize(4);
        assertThat(cells.get(1).labels()).containsExactly("2024", "F");
        assertThat(cells.get(1).status()).isEqualTo("u");
        assertThat(cells.get(3).value()).isNull();
    }

    @Test
    void rejectsMalformedDocuments() {
        for (var invalid : new String[] {
                "not json",
                VALID.replace("\"2.0\"", "\"1.0\""),
                VALID.replace("\"updated\":\"2026-10-19T05:00:00Z\",", ""),
                VALID.replace("\"size\":[2,2]", "\"size\":[2,null]"),
                VALID.replace("\"size\":[2,2]", "\"size\":[2,3]"),
                VALID.replace("\"size\":[2,2]", "\"size\":[2]"),
                VALID.replace("[1,2,3,null]", "[1,2,3]"),
                VALID.replace("\"2025\":1", "\"2025\":0"),
                VALID.replace("\"2025\":1", "\"2025\":5"),
                VALID.replace("\"id\":[\"YEAR\",\"SEX\"]", "\"id\":[\"YEAR\",null]"),
                VALID.replace("\"SEX\":{\"category\"", "\"OTHER\":{\"category\""),
                VALID.replace("\"status\":{\"1\":\"u\"}", "\"status\":{\"one\":\"u\"}")}) {
            assertThatThrownBy(() -> parser.parse(bytes(invalid))).as(invalid)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void rejectsDimensionsWhoseCellCountWouldOverflow() {
        var categories = new StringBuilder();
        for (var index = 0; index < 1_000; index++) categories.append(index == 0 ? "" : ",").append("\"c").append(index)
                .append("\":").append(index);
        var dimension = "{\"category\":{\"index\":{" + categories + "}}}";
        var huge = "{\"version\":\"2.0\",\"class\":\"dataset\",\"updated\":\"2026-10-19T05:00:00Z\","
                + "\"id\":[\"A\",\"B\",\"C\",\"D\"],\"size\":[1000,1000,1000,1000],\"value\":[1],"
                + "\"dimension\":{\"A\":" + dimension + ",\"B\":" + dimension + ",\"C\":" + dimension
                + ",\"D\":" + dimension + "}}";
        assertThatThrownBy(() -> parser.parse(bytes(huge))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void convertsCystatPeriodLabels() {
        assertThat(Observation.period("2024")).isEqualTo("2024");
        assertThat(Observation.period("2024M07")).isEqualTo("2024-07");
        assertThat(Observation.period("2024Q3")).isEqualTo("2024-Q3");
        assertThat(Observation.period("2023 (r)")).isEqualTo("2023");
        for (var invalid : new String[] {"2024M13", "2024Q5", "24", "2024-07", ""}) {
            assertThatThrownBy(() -> Observation.period(invalid)).as(invalid)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    private static byte[] bytes(String json) {
        return json.getBytes(StandardCharsets.UTF_8);
    }
}
