package com.cyprusopen.statistics;

import static com.cyprusopen.statistics.StatisticsConstants.DATASET_PAGE;

import java.util.List;
import java.util.function.Function;

/**
 * A CYSTAT table served by the statistics API, declared in {@link StatisticsDatasets}.
 *
 * @param code       stable API code and storage key
 * @param table      table path below the CYSTAT PxWeb API root
 * @param catalogUrl data.gov.cy dataset page that links to the table
 * @param mapping    maps the table to observations; throws {@link IllegalArgumentException} on an unexpected shape
 */
record CystatDataset(String code, String table, String catalogUrl, Function<JsonStat, List<Observation>> mapping)
        implements StatisticsDataset {

    static CystatDataset of(String code, String table, int catalogId, Function<JsonStat, List<Observation>> mapping) {
        return new CystatDataset(code, table, DATASET_PAGE + catalogId, mapping);
    }

    @Override
    public List<Observation> observations(JsonStat table) {
        return mapping.apply(table);
    }
}
