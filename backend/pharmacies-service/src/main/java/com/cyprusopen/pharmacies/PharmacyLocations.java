package com.cyprusopen.pharmacies;

import com.cyprusopen.common.geo.CyprusBounds;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Reviewed pharmacy coordinates from an optional local CSV. A point applies only while the roster
 * still lists the same district, registration number, address and municipality.
 */
@Component
class PharmacyLocations {
    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader().setSkipHeaderRecord(true).setTrim(true).get();

    private final Map<Key, Position> positions;

    @Autowired
    PharmacyLocations(@Value("${pharmacies.locations-file}") String file) throws IOException {
        var loaded = new HashMap<Key, Position>();
        if (!file.isBlank()) {
            try (var input = Files.newBufferedReader(Path.of(file)); var csv = FORMAT.parse(input)) {
                for (var row : csv) {
                    var key = new Key(row.get("district"), row.get("registrationNumber"), row.get("address"),
                            row.get("municipality"));
                    var position = new Position(Double.parseDouble(row.get("latitude")),
                            Double.parseDouble(row.get("longitude")), row.get("source"));
                    if (!CyprusBounds.contains(position.latitude(), position.longitude())
                            || position.source().isBlank() || loaded.putIfAbsent(key, position) != null) {
                        throw new IllegalArgumentException("Invalid or duplicate pharmacy location");
                    }
                }
            }
        }
        positions = Map.copyOf(loaded);
    }

    Position find(DutyEntry entry) {
        return positions.get(new Key(entry.district(), entry.registrationNumber(), entry.address(),
                entry.municipality()));
    }

    private record Key(String district, String registrationNumber, String address, String municipality) {}

    record Position(double latitude, double longitude, String source) {}
}
