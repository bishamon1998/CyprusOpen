package com.cyprusopen.pharmacies;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Stores the official duty roster. Past days stay available for date lookups. */
@Repository
@DependsOn(PharmaciesDatabase.LIQUIBASE)
class PharmacySchedules {
    private final JdbcClient jdbc;

    @Autowired
    PharmacySchedules(@Qualifier(PharmaciesDatabase.JDBC) JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    List<ScheduledPharmacy> on(LocalDate date) {
        return jdbc.sql(PharmacySql.ENTRIES_ON).param("date", date)
                .query((rs, row) -> new ScheduledPharmacy(
                        new DutyEntry(rs.getObject(1, LocalDate.class), rs.getString(2), rs.getString(3),
                                rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)),
                        rs.getString(10) == null ? null : new PharmacyLocation(rs.getDouble(8), rs.getDouble(9),
                                rs.getString(10), rs.getString(11))))
                .list();
    }

    Instant checkedAt() {
        var checked = jdbc.sql(PharmacySql.CHECKED_AT).query(Timestamp.class).optional();
        return checked.map(Timestamp::toInstant).orElse(null);
    }

    /** Replaces each district's schedule from the first day its new file covers; earlier days are kept. */
    @Transactional(PharmaciesDatabase.TRANSACTIONS)
    void replace(Map<District, List<DutyEntry>> schedules, Instant checkedAt) {
        for (var schedule : schedules.entrySet()) {
            var entries = schedule.getValue();
            var from = entries.stream().map(DutyEntry::date).min(Comparator.naturalOrder()).orElseThrow();
            jdbc.sql(PharmacySql.DELETE_FROM).param("district", schedule.getKey().code()).param("from", from).update();
            jdbc.sql(PharmacySql.INSERT)
                    .param("dates", column(entries, entry -> entry.date().toString()))
                    .param("districts", column(entries, DutyEntry::district))
                    .param("registrations", column(entries, DutyEntry::registrationNumber))
                    .param("names", column(entries, DutyEntry::name))
                    .param("addresses", column(entries, DutyEntry::address))
                    .param("municipalities", column(entries, DutyEntry::municipality))
                    .param("phones", column(entries, DutyEntry::phone))
                    .update();
        }
        jdbc.sql(PharmacySql.MARK_CHECKED).param("checked", Timestamp.from(checkedAt)).update();
    }

    private static String[] column(List<DutyEntry> entries, Function<DutyEntry, String> field) {
        return entries.stream().map(field).toArray(String[]::new);
    }
}
