package com.cyprusopen.statistics;

import static com.cyprusopen.statistics.StatisticsConstants.CATALOG_URL;
import static com.cyprusopen.statistics.StatisticsConstants.NOT_IMPORTED;

import java.time.Instant;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class DatasetController {
    private static final List<Dataset> DATASETS = List.of(
            new Dataset("immigration", "Immigration", "Country of previous residence and year", NOT_IMPORTED, null,
                    CATALOG_URL),
            new Dataset("tourism", "Tourist arrivals", "Country of usual residence and month", NOT_IMPORTED, null,
                    CATALOG_URL));

    @GetMapping("/api/v1/statistics/datasets")
    ResponseEntity<List<Dataset>> datasets() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(DATASETS);
    }

    record Dataset(String code, String title, String breakdown, String availability,
                   Instant lastUpdatedAt, String discoveryUrl) {}
}
