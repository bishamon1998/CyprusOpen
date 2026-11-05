package com.cyprusopen.pharmacies;

import static com.cyprusopen.pharmacies.PharmacyScheduleConstants.DISTRICTS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
class PharmacySchedules {
    private final List<PharmacyCsvReader.DutyEntry> entries;

    @Autowired
    PharmacySchedules(PharmacyCsvReader reader, @Value("${pharmacies.schedules.directory}") String directory)
            throws IOException {
        var loaded = new ArrayList<PharmacyCsvReader.DutyEntry>();
        if (!directory.isBlank()) {
            for (var district : DISTRICTS) {
                var file = Path.of(directory).resolve(district + ".csv");
                if (Files.notExists(file)) continue;
                try (var input = Files.newBufferedReader(file)) {
                    loaded.addAll(reader.read(input, district));
                } catch (IOException | RuntimeException invalid) {
                    throw new IllegalStateException("Could not load duty schedule for " + district, invalid);
                }
            }
        }
        entries = List.copyOf(loaded);
    }

    List<PharmacyCsvReader.DutyEntry> forDate(LocalDate date) {
        return entries.stream().filter(entry -> entry.date().equals(date)).toList();
    }
}
