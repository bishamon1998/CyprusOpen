package com.cyprusopen.places.recycling;

import static com.cyprusopen.places.recycling.RecyclingConstants.MAX_POINTS;

import com.cyprusopen.common.geo.CyprusBounds;
import com.cyprusopen.places.CsvTable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Reads the Green Dot points file. Rows of an unknown category or without a position in Cyprus are skipped; a file
 * without the expected columns or without points of both kinds throws {@link IllegalArgumentException}.
 */
@Component
class RecyclingParser {
    private static final Pattern SPACES = Pattern.compile("\\s+");
    private static final Map<String, String> KINDS = Map.of(
            "Glass Collection Point", "glass",
            "PMD Paper Glass Collection Point", "island");

    RecyclingSnapshot parse(byte[] csv, Instant publishedAt, Instant retrievedAt) {
        var rows = CsvTable.read(csv, List.of("NAME_GR", "CATEGORIES_EN", "DESCRIPTION_GR", "DESCRIPTION_EN",
                "Latitude", "Longitude"));
        var points = new ArrayList<RecyclingSnapshot.Point>();
        for (int index = 0; index < rows.size(); index++) {
            var row = rows.get(index);
            var kind = KINDS.get(row.getOrDefault("CATEGORIES_EN", ""));
            if (kind == null) continue;
            double latitude;
            double longitude;
            try {
                latitude = Double.parseDouble(row.getOrDefault("Latitude", ""));
                longitude = Double.parseDouble(row.getOrDefault("Longitude", ""));
            } catch (NumberFormatException missing) {
                continue;
            }
            if (!CyprusBounds.contains(latitude, longitude)) continue;
            // The Greek description is usually the same as the "English" one; a few points have only the latter.
            var description = text(row.get("DESCRIPTION_GR"));
            if (description == null) description = text(row.get("DESCRIPTION_EN"));
            points.add(new RecyclingSnapshot.Point("point-" + (index + 1), kind, text(row.get("NAME_GR")),
                    description, round(latitude), round(longitude)));
        }
        for (var kind : KINDS.values()) {
            if (points.stream().noneMatch(point -> point.kind().equals(kind))) {
                throw new IllegalArgumentException("No " + kind + " points");
            }
        }
        if (points.size() > MAX_POINTS) throw new IllegalArgumentException("Too many points");
        return new RecyclingSnapshot(retrievedAt, publishedAt, points);
    }

    /** One line without repeated spaces; null when blank or a lone dash. */
    private static String text(String value) {
        if (value == null) return null;
        var line = SPACES.matcher(value).replaceAll(" ").strip();
        return line.isEmpty() || line.equals("-") ? null : line;
    }

    private static double round(double degrees) {
        return Math.round(degrees * 100_000) / 100_000.0;
    }
}
