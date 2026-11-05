package com.cyprusopen.statistics;

import com.cyprusopen.common.refresh.RefreshResult;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/** Refreshes each dataset independently: a failing table keeps its last stored values. */
@Service
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class StatisticsRefresh {
    private final List<StatisticsDataset> datasets;
    private final CystatClient client;
    private final JsonStatParser parser;
    private final ObservationStore store;
    private final Clock clock;

    Map<String, RefreshResult> refreshAll() {
        var results = new LinkedHashMap<String, RefreshResult>();
        datasets.forEach(dataset -> results.put(dataset.code(), refresh(dataset)));
        return results;
    }

    boolean anyMissing() {
        var imported = store.imported();
        return datasets.stream().anyMatch(dataset -> !imported.containsKey(dataset.code()));
    }

    RefreshResult refresh(StatisticsDataset dataset) {
        byte[] body;
        try {
            body = client.table(dataset.table());
        } catch (RestClientException | IllegalArgumentException unavailable) {
            log.warn("Statistics source unavailable for {}: {}", dataset.code(), unavailable.getMessage());
            return RefreshResult.UNAVAILABLE;
        }
        try {
            var table = parser.parse(body);
            var stored = store.imported(dataset.code());
            if (stored.isPresent() && stored.get().sourceUpdatedAt().equals(table.updated())) {
                store.markChecked(dataset.code(), clock.instant());
                return RefreshResult.UNCHANGED;
            }
            var observations = dataset.observations(table);
            if (observations.isEmpty()) throw new IllegalArgumentException("No observations");
            store.replace(dataset.code(), table.updated(), clock.instant(), observations);
            return RefreshResult.UPDATED;
        } catch (IllegalArgumentException invalid) {
            log.warn("Statistics table rejected for {}: {}", dataset.code(), invalid.getMessage());
            return RefreshResult.INVALID;
        }
    }
}
