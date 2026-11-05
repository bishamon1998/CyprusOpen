package com.cyprusopen.pharmacies;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class PharmacyLocations {
    private final Map<Key, Position> positions;

    @Autowired
    PharmacyLocations(@Value("${pharmacies.locations-file}") String file) throws IOException {
        var loaded = new HashMap<Key, Position>();
        if (!file.isBlank()) {
            try (var input = Files.newBufferedReader(Path.of(file));
                 var csv = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).get().parse(input)) {
                for (var row : csv) {
                    var key = new Key(row.get("district"), row.get("registrationNumber"), row.get("address"),
                            row.get("municipality"));
                    var position = new Position(Double.parseDouble(row.get("latitude")),
                            Double.parseDouble(row.get("longitude")), row.get("source"));
                    if (!(position.latitude() >= 34.5 && position.latitude() <= 35.75
                            && position.longitude() >= 32.25 && position.longitude() <= 34.65)
                            || position.source().isBlank() || loaded.putIfAbsent(key, position) != null) {
                        throw new IllegalArgumentException("Invalid or duplicate pharmacy location");
                    }
                }
            }
        }
        positions = Map.copyOf(loaded);
    }

    Map<String, Position> forEntries(List<PharmacyCsvReader.DutyEntry> entries) {
        var result = new HashMap<String, Position>();
        for (var entry : entries) {
            var key = new Key(entry.district(), entry.registrationNumber(), entry.address(), entry.municipality());
            var position = positions.get(key);
            if (position != null) result.put(entry.district() + ":" + entry.registrationNumber(), position);
        }
        return result;
    }

    private record Key(String district, String registrationNumber, String address, String municipality) {}
    record Position(double latitude, double longitude, String source) {}
}
