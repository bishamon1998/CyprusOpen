package com.cyprusopen.pharmacies;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class DutyPeriodsTest {
    private static final ZoneId CYPRUS = ZoneId.of("Asia/Nicosia");

    @Test
    void rosterDayChangesAtEightAndNightsBelongToThePreviousDay() {
        assertThat(at("2026-11-02T07:59")).isEqualTo(new DutyPeriods.Period(LocalDate.of(2026, 11, 1), true));
        assertThat(at("2026-11-02T08:00")).isEqualTo(new DutyPeriods.Period(LocalDate.of(2026, 11, 2), false));
        assertThat(at("2026-01-01T00:30")).isEqualTo(new DutyPeriods.Period(LocalDate.of(2025, 12, 31), true));
    }

    @Test
    void onCallStartsAtTenInWinterAndElevenInSummer() {
        assertThat(at("2026-11-02T21:59").onCall()).isFalse();
        assertThat(at("2026-11-02T22:00").onCall()).isTrue();
        assertThat(at("2026-04-30T22:30").onCall()).isTrue();
        assertThat(at("2026-05-01T22:30").onCall()).isFalse();
        assertThat(at("2026-09-30T22:59").onCall()).isFalse();
        assertThat(at("2026-09-30T23:00").onCall()).isTrue();
        assertThat(at("2026-10-01T22:30").onCall()).isTrue();
    }

    @Test
    void aSelectedDateIsNeverMarkedOnCall() {
        var date = LocalDate.of(2026, 11, 5);
        assertThat(new DutyPeriods(Clock.systemUTC()).on(date)).isEqualTo(new DutyPeriods.Period(date, false));
    }

    private static DutyPeriods.Period at(String cyprusTime) {
        var instant = LocalDateTime.parse(cyprusTime).atZone(CYPRUS).toInstant();
        return new DutyPeriods(Clock.fixed(instant, CYPRUS)).current();
    }
}
