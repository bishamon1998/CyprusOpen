package com.cyprusopen.places.recycling;

import static com.cyprusopen.places.recycling.RecyclingConstants.LICENSE;
import static com.cyprusopen.places.recycling.RecyclingConstants.LICENSE_URL;
import static com.cyprusopen.places.recycling.RecyclingConstants.SOURCE_NAME;
import static com.cyprusopen.places.recycling.RecyclingConstants.SOURCE_URL;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Green Dot glass banks and recycling islands. Serves stored data only. */
@RestController
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class RecyclingController {
    private final RecyclingStore store;

    @GetMapping("/api/v1/recycling")
    ResponseEntity<Response> recycling() {
        var snapshot = store.read();
        var source = new Source(SOURCE_NAME, SOURCE_URL, LICENSE, LICENSE_URL,
                snapshot.map(RecyclingSnapshot::publishedAt).orElse(null),
                snapshot.map(RecyclingSnapshot::retrievedAt).orElse(null));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new Response(source, snapshot.map(RecyclingSnapshot::points).orElse(List.of())));
    }

    record Response(Source source, List<RecyclingSnapshot.Point> points) {}
    /** {@code publishedAt}: when the source last changed its file; {@code retrievedAt}: the last download. */
    record Source(String name, String url, String license, String licenseUrl, Instant publishedAt,
                  Instant retrievedAt) {}
}
