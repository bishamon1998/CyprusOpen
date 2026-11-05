package com.cyprusopen.pharmacies;

import static com.cyprusopen.pharmacies.PharmacyConstants.SOURCE;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class PharmacyController {
    private final PharmacySchedules schedules;
    private final DutyPeriods periods;

    @GetMapping("/api/v1/pharmacies")
    ResponseEntity<Schedule> schedule(@RequestParam(required = false) LocalDate date) {
        var period = date == null ? periods.current() : periods.on(date);
        var items = schedules.on(period.date()).stream().map(PharmacyController::item).toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new Schedule(period.date(), SOURCE, schedules.checkedAt(), period.onCall(), items));
    }

    private static Pharmacy item(ScheduledPharmacy scheduled) {
        var entry = scheduled.entry();
        return new Pharmacy(entry.district() + ":" + entry.registrationNumber(), entry.district(),
                entry.registrationNumber(), entry.name(), entry.address(), entry.municipality(), entry.phone(),
                scheduled.location());
    }

    record Schedule(LocalDate date, String source, Instant checkedAt, boolean onCall, List<Pharmacy> items) {}

    record Pharmacy(String id, String district, String registrationNumber, String name, String address,
                    String municipality, String phone, PharmacyLocation location) {}
}
