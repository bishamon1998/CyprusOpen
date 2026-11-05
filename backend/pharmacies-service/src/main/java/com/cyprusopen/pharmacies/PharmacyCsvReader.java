package com.cyprusopen.pharmacies;

import static com.cyprusopen.pharmacies.PharmacyScheduleConstants.LONG_DATE;
import static com.cyprusopen.pharmacies.PharmacyScheduleConstants.SHORT_DATE;

import java.io.IOException;
import java.io.Reader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.springframework.stereotype.Component;

@Component
class PharmacyCsvReader {
    List<DutyEntry> read(Reader reader, String district) throws IOException {
        var entries = new ArrayList<DutyEntry>();
        try (var csv = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).get().parse(reader)) {
            var headers = csv.getHeaderNames();
            if (headers.isEmpty() || !headers.getFirst().replace("\uFEFF", "").equals("Date")
                    || !headers.containsAll(List.of("Reg. No.", "Name", "Surmame", "Address",
                            "Muniuciplity / Community", "Pharmacy Tel. No."))) {
                throw new IllegalArgumentException("Unexpected duty schedule headers");
            }
            for (var row : csv) {
                if (row.get(0).isBlank()) continue;
                var date = row.get(0);
                var id = row.get("Reg. No.");
                var name = (row.get("Name") + " " + row.get("Surmame")).strip();
                var address = row.get("Address");
                if (!id.matches("[0-9]+") || name.isBlank() || address.isBlank()) {
                    throw new IllegalArgumentException("Invalid duty entry at row " + row.getRecordNumber());
                }
                entries.add(new DutyEntry(LocalDate.parse(date, date.matches(".*/[0-9]{2}") ? SHORT_DATE : LONG_DATE),
                        district, id, name, address, row.get("Muniuciplity / Community"),
                        row.get("Pharmacy Tel. No.")));
            }
        }
        return List.copyOf(entries);
    }

    record DutyEntry(LocalDate date, String district, String registrationNumber, String name,
                     String address, String municipality, String phone) {}
}
