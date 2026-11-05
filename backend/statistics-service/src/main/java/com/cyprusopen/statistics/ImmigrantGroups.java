package com.cyprusopen.statistics;

import static com.cyprusopen.statistics.StatisticsConstants.DATASET_PAGE;
import static com.cyprusopen.statistics.StatisticsConstants.TOTAL_SERIES;
import static java.util.Map.entry;

import java.util.List;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Long-term immigrants to Cyprus by citizenship, country of birth and country of previous residence, annual.
 * CYSTAT publishes only Cyprus / EU / non-EU groups here, not individual countries.
 */
@Component
@Order(1)
class ImmigrantGroups implements StatisticsDataset {
    private static final String GROUP = "CITIZENSHIP, COUNTRY OF BIRTH, COUNTRY OF PREVIOUS RESIDENCE";
    private static final String YEAR = "YEAR";
    private static final Map<String, String> PINNED = Map.of("SEX", "Total", "AGE", "Total");
    private static final Map<String, String> SERIES = Map.ofEntries(
            entry("Total Immigrants", TOTAL_SERIES),
            entry("Citizenship - Cypriots", "citizenship-cyprus"),
            entry("Citizenship - EU", "citizenship-eu"),
            entry("Citizenship - Non EU", "citizenship-non-eu"),
            entry("Country of birth - Cyprus", "birth-cyprus"),
            entry("Country of birth - EU", "birth-eu"),
            entry("Country of birth - Non EU", "birth-non-eu"),
            entry("Country of previous residence - EU", "previous-residence-eu"),
            entry("Country of previous residence - Non EU", "previous-residence-non-eu"));

    @Override
    public String code() {
        return "immigrant-groups";
    }

    @Override
    public String table() {
        return "Population/Migration/1840030E.px";
    }

    @Override
    public String catalogUrl() {
        return DATASET_PAGE + "2174";
    }

    @Override
    public List<Observation> observations(JsonStat table) {
        return SeriesMapping.map(table, GROUP, SeriesMapping.labels(SERIES), YEAR, PINNED);
    }
}
