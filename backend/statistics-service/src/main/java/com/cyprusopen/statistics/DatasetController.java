package com.cyprusopen.statistics;

import java.time.Instant;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class DatasetController {
    private static final String CATALOG = "https://www.data.gov.cy/en/search?s=&f%5B0%5D=content_type%3Adataset.dataset";

    @GetMapping("/api/v1/statistics/datasets")
    ResponseEntity<List<Dataset>> datasets() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(List.of(
                new Dataset("immigration", "Immigration", "Country of previous residence and year", "not_imported", null, CATALOG),
                new Dataset("tourism", "Tourist arrivals", "Country of usual residence and month", "not_imported", null, CATALOG)));
    }

    record Dataset(String code, String title, String breakdown, String availability,
                   Instant lastUpdatedAt, String discoveryUrl) {}
}
