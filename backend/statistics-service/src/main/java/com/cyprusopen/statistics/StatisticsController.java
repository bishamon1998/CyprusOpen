package com.cyprusopen.statistics;

import static com.cyprusopen.statistics.StatisticsConstants.LICENSE;
import static com.cyprusopen.statistics.StatisticsConstants.LICENSE_URL;
import static com.cyprusopen.statistics.StatisticsConstants.SOURCE_NAME;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class StatisticsController {
    private static final CacheControl DATA_CACHE = CacheControl.maxAge(Duration.ofHours(1)).cachePublic();

    private final List<StatisticsDataset> datasets;
    private final ObservationStore store;

    @GetMapping("/api/v1/statistics/datasets")
    ResponseEntity<List<Source>> datasets() {
        var imported = store.imported();
        var sources = datasets.stream().map(dataset -> source(dataset, imported.get(dataset.code()))).toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(sources);
    }

    @GetMapping("/api/v1/statistics/datasets/{code}")
    ResponseEntity<Series> dataset(@PathVariable String code) {
        var dataset = datasets.stream().filter(candidate -> candidate.code().equals(code)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var source = source(dataset, store.imported(code).orElse(null));
        return ResponseEntity.ok().cacheControl(DATA_CACHE).body(series(source, store.observations(code)));
    }

    private static Source source(StatisticsDataset dataset, ObservationStore.Imported imported) {
        return new Source(dataset.code(), SOURCE_NAME, dataset.catalogUrl(), LICENSE, LICENSE_URL,
                imported == null ? null : imported.sourceUpdatedAt(), imported == null ? null : imported.checkedAt());
    }

    /** Aligns observations on one shared, sorted period axis; missing values are null. */
    private static Series series(Source source, List<Observation> observations) {
        var periods = new ArrayList<>(new TreeSet<>(observations.stream().map(Observation::period).toList()));
        var columns = new LinkedHashMap<String, Integer>();
        for (var index = 0; index < periods.size(); index++) columns.put(periods.get(index), index);
        var rows = new LinkedHashMap<String, Line>();
        for (var observation : observations) {
            var line = rows.computeIfAbsent(observation.series(),
                    key -> new Line(key, new ArrayList<>(Collections.nCopies(periods.size(), (Double) null)),
                            new ArrayList<>()));
            var column = columns.get(observation.period());
            line.values().set(column, observation.value());
            if (observation.lowReliability()) line.lowReliability().add(column);
        }
        return new Series(source, periods, List.copyOf(rows.values()));
    }

    record Source(String code, String name, String url, String license, String licenseUrl,
                  Instant updatedAt, Instant checkedAt) {}

    /** {@code lowReliability} lists period indexes CYSTAT marks as low reliability. */
    record Line(String key, List<Double> values, List<Integer> lowReliability) {}

    record Series(Source source, List<String> periods, List<Line> series) {}
}
