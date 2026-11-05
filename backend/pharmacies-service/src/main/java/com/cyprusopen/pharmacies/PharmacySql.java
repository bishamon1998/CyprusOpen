package com.cyprusopen.pharmacies;

final class PharmacySql {
    static final String ENTRIES_ON = """
            SELECT duty_date, district, registration_number, name, address, municipality, phone
            FROM duty_entry WHERE duty_date = :date
            ORDER BY district, name, registration_number
            """;
    static final String CHECKED_AT = "SELECT last_checked_at FROM schedule_refresh WHERE id = 1";
    static final String DELETE_FROM = "DELETE FROM duty_entry WHERE district = :district AND duty_date >= :from";
    static final String INSERT = """
            INSERT INTO duty_entry (duty_date, district, registration_number, name, address, municipality, phone)
            SELECT * FROM unnest(CAST(:dates AS date[]), CAST(:districts AS text[]), CAST(:registrations AS text[]),
                CAST(:names AS text[]), CAST(:addresses AS text[]), CAST(:municipalities AS text[]),
                CAST(:phones AS text[]))
            """;
    static final String MARK_CHECKED = "UPDATE schedule_refresh SET last_checked_at = :checked WHERE id = 1";

    private PharmacySql() {}
}
