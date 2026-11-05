package com.cyprusopen.parking;

import static com.cyprusopen.parking.DirectoryConstants.UNAVAILABLE;
import static com.cyprusopen.parking.DirectoryConstants.UNNAMED_LOCATION;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ParkingController {
    private final ParkingDirectory directory;

    @GetMapping("/api/v1/parking")
    ResponseEntity<ParkingResponse> parking() {
        var stored = directory.read();
        var snapshot = stored.snapshot();
        var source = new DirectorySource(snapshot.source(), snapshot.catalogUrl(), snapshot.license(),
                snapshot.licenseUrl(), snapshot.publishedAt(), snapshot.retrievedAt(), stored.checkedAt());
        var places = snapshot.items().stream().map(place -> new ParkingPlace(place.id(),
                place.name() == null ? UNNAMED_LOCATION : place.name(), place.latitude(), place.longitude(), null)).toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new ParkingResponse(UNAVAILABLE, null, source, places));
    }

    record ParkingResponse(String freshness, Instant lastUpdatedAt, DirectorySource directory,
                           List<ParkingPlace> items) {}
    record DirectorySource(String name, String url, String license, String licenseUrl,
                           Instant publishedAt, Instant retrievedAt, Instant checkedAt) {}
    record ParkingPlace(String id, String name, double latitude, double longitude, Integer availableSpaces) {}
}
