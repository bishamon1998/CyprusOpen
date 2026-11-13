package com.cyprusopen.places.services;

import static com.cyprusopen.places.services.ServicesConstants.CENTRES_SOURCE_NAME;
import static com.cyprusopen.places.services.ServicesConstants.CENTRES_SOURCE_URL;
import static com.cyprusopen.places.services.ServicesConstants.HOSPITALS_SOURCE_NAME;
import static com.cyprusopen.places.services.ServicesConstants.HOSPITALS_SOURCE_URL;
import static com.cyprusopen.places.services.ServicesConstants.LICENSE;
import static com.cyprusopen.places.services.ServicesConstants.LICENSE_URL;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** State hospitals and Citizen Service Centres. Serves stored data only. */
@RestController
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ServicesController {
    private final ServicesStore store;

    @GetMapping("/api/v1/services")
    ResponseEntity<Response> services() {
        var snapshot = store.read();
        var retrievedAt = snapshot.map(ServicesSnapshot::retrievedAt).orElse(null);
        var sources = new Sources(
                new Source(HOSPITALS_SOURCE_NAME, HOSPITALS_SOURCE_URL, LICENSE, LICENSE_URL,
                        snapshot.map(ServicesSnapshot::hospitalsPublishedAt).orElse(null), retrievedAt),
                new Source(CENTRES_SOURCE_NAME, CENTRES_SOURCE_URL, LICENSE, LICENSE_URL,
                        snapshot.map(ServicesSnapshot::centresPublishedAt).orElse(null), retrievedAt));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new Response(sources, snapshot.map(ServicesSnapshot::places).orElse(List.of())));
    }

    record Response(Sources sources, List<ServicesSnapshot.Place> places) {}
    record Sources(Source hospitals, Source centres) {}
    /** {@code publishedAt}: when the source last changed its file; {@code retrievedAt}: the last download. */
    record Source(String name, String url, String license, String licenseUrl, Instant publishedAt,
                  Instant retrievedAt) {}
}
