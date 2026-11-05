package com.cyprusopen.statistics;

import static com.cyprusopen.statistics.StatisticsConstants.DATASET_PAGE;
import static com.cyprusopen.statistics.StatisticsConstants.TOTAL_SERIES;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Arrivals of tourists by country of usual residence, monthly. Series are ISO country codes plus the total. */
@Component
@Order(3)
class TouristArrivals implements StatisticsDataset {
    private static final String COUNTRY = "COUNTRY OF USUAL RESIDENCE";
    private static final String MONTH = "MONTH";
    private static final String TOTAL = "TOTAL";

    @Override
    public String code() {
        return "tourist-arrivals";
    }

    @Override
    public String table() {
        return "Tourism/Tourists/Monthly/2021010E.px";
    }

    @Override
    public String catalogUrl() {
        return DATASET_PAGE + "2285";
    }

    @Override
    public List<Observation> observations(JsonStat table) {
        return SeriesMapping.map(table, COUNTRY,
                label -> TOTAL.equals(label) ? Optional.of(TOTAL_SERIES) : CystatCountries.code(label),
                MONTH, Map.of());
    }
}
