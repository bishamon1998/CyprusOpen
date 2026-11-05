package com.cyprusopen.statistics;

import static com.cyprusopen.statistics.StatisticsConstants.DATASET_PAGE;

import java.util.List;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Long-term immigrants, emigrants and net migration for Cyprus, annual since 1981. */
@Component
@Order(2)
class MigrationMovements implements StatisticsDataset {
    private static final String MOVEMENT = "MIGRATION MOVEMENTS";
    private static final String YEAR = "YEAR";
    private static final Map<String, String> SERIES = Map.of(
            "Total of long term immigrants", "immigrants",
            "Emigrants", "emigrants",
            "Net migration", "net-migration");

    @Override
    public String code() {
        return "migration-movements";
    }

    @Override
    public String table() {
        return "Population/Migration/1840010E.px";
    }

    @Override
    public String catalogUrl() {
        return DATASET_PAGE + "2172";
    }

    @Override
    public List<Observation> observations(JsonStat table) {
        return SeriesMapping.map(table, MOVEMENT, SeriesMapping.labels(SERIES), YEAR, Map.of());
    }
}
