package com.cyprusopen.places.beach;

import static com.cyprusopen.places.beach.BeachConstants.LICENSE;
import static com.cyprusopen.places.beach.BeachConstants.LICENSE_URL;
import static com.cyprusopen.places.beach.BeachConstants.SOURCE_NAME;
import static com.cyprusopen.places.beach.BeachConstants.SOURCE_URL;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Designated bathing waters with their water quality classification. Serves stored data only. */
@RestController
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class BeachController {
    private final BeachStore store;

    @GetMapping("/api/v1/beaches")
    ResponseEntity<Response> beaches() {
        var snapshot = store.read();
        var source = new Source(SOURCE_NAME, SOURCE_URL, LICENSE, LICENSE_URL,
                snapshot.map(BeachSnapshot::retrievedAt).orElse(null));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Response(source,
                snapshot.map(BeachSnapshot::season).orElse(null),
                snapshot.map(BeachSnapshot::beaches).orElse(List.of())));
    }

    record Response(Source source, Integer season, List<BeachSnapshot.Beach> beaches) {}
    record Source(String name, String url, String license, String licenseUrl, Instant retrievedAt) {}
}
