package com.cyprusopen.parking;

import java.time.Instant;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ParkingController {
    @GetMapping("/api/v1/parking")
    ResponseEntity<ParkingResponse> parking() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new ParkingResponse("unavailable", null, List.of()));
    }

    record ParkingResponse(String freshness, Instant lastUpdatedAt, List<ParkingPlace> items) {}
    record ParkingPlace(String id, String name, Integer availableSpaces) {}
}
