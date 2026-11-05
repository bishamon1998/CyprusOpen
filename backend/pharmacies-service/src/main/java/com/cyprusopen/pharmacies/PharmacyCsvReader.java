package com.cyprusopen.pharmacies;

import static com.cyprusopen.pharmacies.PharmacyConstants.ADDRESS_COLUMN;
import static com.cyprusopen.pharmacies.PharmacyConstants.BYTE_ORDER_MARK;
import static com.cyprusopen.pharmacies.PharmacyConstants.DATE_COLUMN;
import static com.cyprusopen.pharmacies.PharmacyConstants.FIRST_NAME_COLUMN;
import static com.cyprusopen.pharmacies.PharmacyConstants.LONG_DATE;
import static com.cyprusopen.pharmacies.PharmacyConstants.MUNICIPALITY_COLUMN;
import static com.cyprusopen.pharmacies.PharmacyConstants.PHONE_COLUMN;
import static com.cyprusopen.pharmacies.PharmacyConstants.REGISTRATION_COLUMN;
import static com.cyprusopen.pharmacies.PharmacyConstants.REGISTRATION_PATTERN;
import static com.cyprusopen.pharmacies.PharmacyConstants.REQUIRED_COLUMNS;
import static com.cyprusopen.pharmacies.PharmacyConstants.SHORT_DATE;
import static com.cyprusopen.pharmacies.PharmacyConstants.SHORT_YEAR_PATTERN;
import static com.cyprusopen.pharmacies.PharmacyConstants.SURNAME_COLUMN;

import java.io.IOException;
import java.io.Reader;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

/** Maps one district duty schedule CSV to entries. */
@Component
class PharmacyCsvReader {
    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader().setSkipHeaderRecord(true).setTrim(true).get();

    List<DutyEntry> read(Reader reader, District district) throws IOException {
        var entries = new ArrayList<DutyEntry>();
        var keys = new HashSet<String>();
        try (var csv = FORMAT.parse(reader)) {
            var headers = csv.getHeaderNames();
            if (headers.isEmpty() || !headers.getFirst().replace(BYTE_ORDER_MARK, "").equals(DATE_COLUMN)
                    || !headers.containsAll(REQUIRED_COLUMNS)) {
                throw new IllegalArgumentException("Unexpected duty schedule headers");
            }
            for (var row : csv) {
                if (row.get(0).isBlank()) continue;
                var entry = entry(row, district);
                if (!keys.add(entry.date() + ":" + entry.registrationNumber())) {
                    throw new IllegalArgumentException("Duplicate duty entry at row " + row.getRecordNumber());
                }
                entries.add(entry);
            }
        }
        return List.copyOf(entries);
    }

    private DutyEntry entry(CSVRecord row, District district) {
        var id = row.get(REGISTRATION_COLUMN);
        var name = (row.get(FIRST_NAME_COLUMN) + " " + row.get(SURNAME_COLUMN)).strip();
        var address = row.get(ADDRESS_COLUMN);
        if (!id.matches(REGISTRATION_PATTERN) || name.isBlank() || address.isBlank()) {
            throw new IllegalArgumentException("Invalid duty entry at row " + row.getRecordNumber());
        }
        return new DutyEntry(date(row.get(0)), district.code(), id, name, address, row.get(MUNICIPALITY_COLUMN),
                row.get(PHONE_COLUMN));
    }

    private LocalDate date(String value) {
        try {
            return LocalDate.parse(value, value.matches(SHORT_YEAR_PATTERN) ? SHORT_DATE : LONG_DATE);
        } catch (DateTimeException invalid) {
            throw new IllegalArgumentException("Invalid duty date", invalid);
        }
    }
}
