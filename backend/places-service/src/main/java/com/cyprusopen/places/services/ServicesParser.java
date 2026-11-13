package com.cyprusopen.places.services;

import static com.cyprusopen.places.services.ServicesConstants.MAX_PLACES;

import com.cyprusopen.common.geo.CyprusBounds;
import com.cyprusopen.places.CsvTable;
import java.net.URI;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

/**
 * Reads the state hospital list and the Citizen Service Centre list (Greek CSV files with coordinates). Rows without
 * a name or a position in Cyprus are skipped; a file without the expected columns or without any usable row throws
 * {@link IllegalArgumentException}.
 */
@Component
class ServicesParser {
    private static final Pattern SPACES = Pattern.compile("\\s+");
    /** "Δευτερα - Παρασκευή, 8:00 - 17:00" or "Πέμπτη, 09:00 - 13:45". */
    private static final Pattern HOURS = Pattern.compile(
            "(\\p{L}+)(?:\\s*-\\s*(\\p{L}+))?\\s*,\\s*(\\d{1,2})[:.](\\d{2})\\s*-\\s*(\\d{1,2})[:.](\\d{2})");
    private static final Map<String, Integer> DAYS = Map.of("δευτερα", 1, "τριτη", 2, "τεταρτη", 3, "πεμπτη", 4,
            "παρασκευη", 5, "σαββατο", 6, "κυριακη", 7);

    record Files(byte[] hospitals, byte[] centres, Instant hospitalsPublishedAt, Instant centresPublishedAt) {}

    ServicesSnapshot parse(Files files, Instant retrievedAt) {
        var places = new ArrayList<ServicesSnapshot.Place>();
        places.addAll(hospitals(files.hospitals()));
        places.addAll(centres(files.centres()));
        for (var kind : List.of("hospital", "centre")) {
            if (places.stream().noneMatch(place -> place.kind().equals(kind))) {
                throw new IllegalArgumentException("No " + kind + " with a position");
            }
        }
        if (places.size() > MAX_PLACES) throw new IllegalArgumentException("Too many places");
        return new ServicesSnapshot(retrievedAt, files.hospitalsPublishedAt(), files.centresPublishedAt(), places);
    }

    private static List<ServicesSnapshot.Place> hospitals(byte[] csv) {
        var rows = CsvTable.read(csv, List.of("Hospital", "Address", "Tel", "latitude", "longitude"));
        var places = new ArrayList<ServicesSnapshot.Place>();
        IntStream.range(0, rows.size()).forEach(index -> {
            var row = rows.get(index);
            var position = position(row);
            var name = text(row.get("Hospital"));
            if (position == null || name == null) return;
            places.add(new ServicesSnapshot.Place("hospital-" + (index + 1), "hospital", name, text(row.get("Address")),
                    text(row.get("Tel")), null, null, null, position[0], position[1]));
        });
        return places;
    }

    private static List<ServicesSnapshot.Place> centres(byte[] csv) {
        var rows = CsvTable.read(csv,
                List.of("CSC_Name", "Address", "Office_Tel", "Opening_Hours", "Appointment_URL", "latitude", "longitude"));
        var places = new ArrayList<ServicesSnapshot.Place>();
        IntStream.range(0, rows.size()).forEach(index -> {
            var row = rows.get(index);
            var position = position(row);
            var name = text(row.get("CSC_Name"));
            if (position == null || name == null) return;
            var hoursText = text(row.get("Opening_Hours"));
            places.add(new ServicesSnapshot.Place("centre-" + (index + 1), "centre", name, text(row.get("Address")),
                    text(row.get("Office_Tel")), hours(hoursText), hoursText, webAddress(row.get("Appointment_URL")),
                    position[0], position[1]));
        });
        return places;
    }

    /** Opening hours from the published text, or null when it is not one range of days with one time range. */
    static ServicesSnapshot.Hours hours(String text) {
        if (text == null) return null;
        var match = HOURS.matcher(text.strip());
        if (!match.matches()) return null;
        var first = DAYS.get(normalized(match.group(1)));
        var last = match.group(2) == null ? first : DAYS.get(normalized(match.group(2)));
        if (first == null || last == null || last < first) return null;
        int opensHour = Integer.parseInt(match.group(3));
        int closesHour = Integer.parseInt(match.group(5));
        if (opensHour > 23 || closesHour > 23) return null;
        var opens = "%02d:%s".formatted(opensHour, match.group(4));
        var closes = "%02d:%s".formatted(closesHour, match.group(6));
        if (opens.compareTo(closes) >= 0) return null;
        return new ServicesSnapshot.Hours(IntStream.rangeClosed(first, last).boxed().toList(), opens, closes);
    }

    private static double[] position(Map<String, String> row) {
        try {
            var latitude = Double.parseDouble(row.getOrDefault("latitude", "").replace(',', '.'));
            var longitude = Double.parseDouble(row.getOrDefault("longitude", "").replace(',', '.'));
            return CyprusBounds.contains(latitude, longitude) ? new double[] {latitude, longitude} : null;
        } catch (NumberFormatException missing) {
            return null;
        }
    }

    /** Only absolute http(s) links are kept. */
    private static String webAddress(String value) {
        var text = text(value);
        if (text == null) return null;
        try {
            var uri = URI.create(text);
            return ("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) && uri.getHost() != null
                    ? uri.toString() : null;
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    /** One line without repeated spaces; null when blank. */
    private static String text(String value) {
        if (value == null) return null;
        var line = SPACES.matcher(value).replaceAll(" ").strip();
        return line.isEmpty() ? null : line;
    }

    private static String normalized(String word) {
        return Normalizer.normalize(word, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
