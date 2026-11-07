package com.cyprusopen.statistics;

import com.cyprusopen.common.refresh.RefreshJob;
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

/**
 * Refreshes each dataset independently: a failing table keeps its last stored values. A table is replaced only
 * when CYSTAT reports a newer update time than the stored one.
 */
@Service
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class StatisticsRefresh implements RefreshJob {
    private final List<StatisticsDataset> datasets;
    private final CystatClient client;
    private final JsonStatParser parser;
    private final ObservationStore store;
    private final Clock clock;

    @Override
    public String name() {
        return "statistics";
    }

    /** Runs after start-up when any dataset has never been imported. */
    @Override
    public boolean needsInitialRun() {
        var imported = store.imported();
        return datasets.stream().anyMatch(dataset -> !imported.containsKey(dataset.code()));
    }

    @Override
    public RefreshResult refresh() {
        var results = refreshAll();
        log.info("Statistics datasets: {}", results);
        return RefreshResult.combine(results.values());
    }

    Map<String, RefreshResult> refreshAll() {
        var results = new LinkedHashMap<String, RefreshResult>();
        datasets.forEach(dataset -> results.put(dataset.code(), refresh(dataset)));
        return results;
    }

    /** Never throws: a database error or a mapping bug in one table must not stop the remaining tables. */
    RefreshResult refresh(StatisticsDataset dataset) {
        try {
            return fetchAndStore(dataset);
        } catch (RuntimeException unexpected) {
            log.error("Statistics refresh failed unexpectedly for {}", dataset.code(), unexpected);
            return RefreshResult.UNAVAILABLE;
        }
    }

    private RefreshResult fetchAndStore(StatisticsDataset dataset) {
        byte[] body;
        try {
            body = client.table(dataset.table());
        } catch (RestClientException | IllegalArgumentException unavailable) {
            log.warn("Statistics source unavailable for {}: {}", dataset.code(), unavailable.getMessage());
            return RefreshResult.UNAVAILABLE;
        }
        try {
            var table = parser.parse(body);
            var stored = store.imported(dataset.code()).map(ObservationStore.Imported::sourceUpdatedAt);
            if (stored.isPresent() && stored.get().equals(table.updated())) {
                store.markChecked(dataset.code(), clock.instant());
                return RefreshResult.UNCHANGED;
            }
            if (stored.isPresent() && table.updated().isBefore(stored.get())) {
                throw new IllegalArgumentException("Table update time " + table.updated()
                        + " is older than the stored " + stored.get());
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
