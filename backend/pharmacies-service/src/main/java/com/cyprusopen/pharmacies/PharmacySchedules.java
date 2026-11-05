package com.cyprusopen.pharmacies;

import static com.cyprusopen.pharmacies.PharmacyScheduleConstants.DISTRICTS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
class PharmacySchedules {
    private volatile Snapshot snapshot;

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
        snapshot = new Snapshot(null, List.copyOf(loaded));
    }

    Snapshot snapshot() {
        return snapshot;
    }

    void replace(Snapshot next) {
        snapshot = next;
    }

    record Snapshot(Instant checkedAt, List<PharmacyCsvReader.DutyEntry> entries) {}
}
