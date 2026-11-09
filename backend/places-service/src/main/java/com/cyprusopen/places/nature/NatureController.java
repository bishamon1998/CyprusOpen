package com.cyprusopen.places.nature;

import static com.cyprusopen.places.nature.NatureConstants.LICENSE;
import static com.cyprusopen.places.nature.NatureConstants.LICENSE_URL;
import static com.cyprusopen.places.nature.NatureConstants.SOURCE_NAME;
import static com.cyprusopen.places.nature.NatureConstants.SOURCE_URL;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Picnic sites, camping sites and nature trails in the state forests. Serves stored data only. */
@RestController
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class NatureController {
    private final NatureStore store;

    @GetMapping("/api/v1/nature")
    ResponseEntity<Response> nature() {
        var snapshot = store.read();
        var source = new Source(SOURCE_NAME, SOURCE_URL, LICENSE, LICENSE_URL,
                snapshot.map(NatureSnapshot::retrievedAt).orElse(null));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new Response(source, snapshot.map(NatureSnapshot::sites).orElse(List.of())));
    }

    record Response(Source source, List<NatureSnapshot.Site> sites) {}
    record Source(String name, String url, String license, String licenseUrl, Instant retrievedAt) {}
}
