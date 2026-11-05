package com.cyprusopen.pharmacies;

import static com.cyprusopen.pharmacies.PharmacyScheduleConstants.CYPRUS;
import static com.cyprusopen.pharmacies.PharmacyScheduleConstants.SOURCE;

import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PharmacyController {
    private final PharmacySchedules schedules;

    @Autowired
    PharmacyController(PharmacySchedules schedules) {
        this.schedules = schedules;
    }

    @GetMapping("/api/v1/pharmacies")
    ResponseEntity<Schedule> get(@RequestParam(required = false) LocalDate date) {
        var day = date == null ? LocalDate.now(CYPRUS) : date;
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new Schedule(day, SOURCE, schedules.forDate(day)));
    }

    record Schedule(LocalDate date, String source, List<PharmacyCsvReader.DutyEntry> items) {}
}
