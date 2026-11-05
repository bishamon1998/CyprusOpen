package com.cyprusopen.pharmacies;

import static com.cyprusopen.pharmacies.PharmacyScheduleConstants.CYPRUS;
import static com.cyprusopen.pharmacies.PharmacyScheduleConstants.SOURCE;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PharmacyController {
    private final PharmacySchedules schedules;
    private final PharmacyLocations locations;

    @Autowired
    PharmacyController(PharmacySchedules schedules, PharmacyLocations locations) {
        this.schedules = schedules;
        this.locations = locations;
    }

    @GetMapping("/api/v1/pharmacies")
    ResponseEntity<Schedule> get(@RequestParam(required = false) LocalDate date) {
        var now = ZonedDateTime.now(CYPRUS);
        var time = now.toLocalTime();
        var today = now.toLocalDate();
        var overnight = time.isBefore(LocalTime.of(8, 0));
        var day = date == null ? (overnight ? today.minusDays(1) : today) : date;
        var summer = today.getMonthValue() >= 5 && today.getMonthValue() <= 9;
        var onCall = date == null && (overnight || !time.isBefore(LocalTime.of(summer ? 23 : 22, 0)));
        var snapshot = schedules.snapshot();
        var items = snapshot.entries().stream().filter(entry -> entry.date().equals(day)).toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new Schedule(day, SOURCE, snapshot.checkedAt(), onCall, items, locations.forEntries(items)));
    }

    record Schedule(LocalDate date, String source, Instant checkedAt, boolean onCall,
                    List<PharmacyCsvReader.DutyEntry> items, Map<String, PharmacyLocations.Position> locations) {}
}
