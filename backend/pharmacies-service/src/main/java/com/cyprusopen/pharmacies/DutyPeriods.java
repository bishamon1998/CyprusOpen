package com.cyprusopen.pharmacies;

import static com.cyprusopen.pharmacies.PharmacyConstants.CYPRUS;
import static com.cyprusopen.pharmacies.PharmacyConstants.ROSTER_CHANGE;
import static com.cyprusopen.pharmacies.PharmacyConstants.SUMMER_END;
import static com.cyprusopen.pharmacies.PharmacyConstants.SUMMER_ON_CALL_START;
import static com.cyprusopen.pharmacies.PharmacyConstants.SUMMER_START;
import static com.cyprusopen.pharmacies.PharmacyConstants.WINTER_ON_CALL_START;

import java.time.Clock;
import java.time.LocalDate;
import java.time.MonthDay;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Decides which roster day is current in Cyprus and whether service is overnight on call. */
@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class DutyPeriods {
    private final Clock clock;

    Period current() {
        var now = clock.instant().atZone(CYPRUS);
        var time = now.toLocalTime();
        var today = now.toLocalDate();
        var overnight = time.isBefore(ROSTER_CHANGE);
        var day = MonthDay.from(today);
        var summer = !day.isBefore(SUMMER_START) && !day.isAfter(SUMMER_END);
        var onCallStart = summer ? SUMMER_ON_CALL_START : WINTER_ON_CALL_START;
        return new Period(overnight ? today.minusDays(1) : today, overnight || !time.isBefore(onCallStart));
    }

    Period on(LocalDate date) {
        return new Period(date, false);
    }

    record Period(LocalDate date, boolean onCall) {}
}
