package com.cyprusopen.places.nature;

import static com.cyprusopen.places.nature.NatureConstants.KML_NAMESPACE;
import static com.cyprusopen.places.nature.NatureConstants.MAX_FILE_BYTES;
import static com.cyprusopen.places.nature.NatureConstants.MAX_SITES;
import static com.cyprusopen.places.nature.NatureConstants.ROUTE_TOLERANCE_DEGREES;
import static com.cyprusopen.places.nature.NatureConstants.SAME_SITE_METRES;
import static com.cyprusopen.places.nature.NatureConstants.TRAIL_NAME_CUT_BYTES;

import com.cyprusopen.common.geo.CyprusBounds;
import com.cyprusopen.common.xml.XmlDocumentReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

/**
 * Reads the Department of Forests KML/KMZ layers. Their attributes sit in an HTML table inside each placemark's
 * description; Greek names there are cut at a byte limit and are completed from the fuller camping catalogue and
 * trail starting points when exactly one name matches. Throws {@link IllegalArgumentException} when a file is not
 * the expected layer or a kind of site is missing.
 */
@Component
class NatureParser {
    private static final Pattern ATTRIBUTE = Pattern.compile("<td>([^<]*)</td>\\s*<td>([^<]*)</td>");
    private static final Pattern SPACES = Pattern.compile("[\\s_]+");
    private static final String CAMPING_NAME = "CampingSite_Name";
    private static final String TRAIL_NAME = "Trail_Name";
    private static final Pattern GREEK = Pattern.compile("\\p{IsGreek}");
    /** The camping layer cuts Greek names at 13 characters ("Ακτή του Κυβε"); an uncompleted one gets "…". */
    private static final int CAMPING_NAME_LIMIT = 13;
    private final XmlDocumentReader reader = new XmlDocumentReader(MAX_FILE_BYTES);

    record Files(byte[] picnic, byte[] campingKmz, byte[] campingCatalogue, byte[] trailRoutes, byte[] trailStarts,
                 byte[] trailCatalogue) {}

    private record Placemark(String id, Map<String, String> attributes, List<List<double[]>> lines) {
        String attribute(String key) {
            var value = attributes.get(key);
            return value == null || value.isBlank() ? null : value.strip();
        }

        double[] first() {
            return lines.getFirst().getFirst();
        }
    }

    NatureSnapshot parse(Files files, Instant retrievedAt) {
        var sites = new ArrayList<NatureSnapshot.Site>();
        sites.addAll(picnicSites(placemarks(files.picnic())));
        sites.addAll(campingSites(placemarks(unzipKml(files.campingKmz())),
                catalogue(files.campingCatalogue(), CAMPING_NAME)));
        // Full Greek trail names: the starting points layer and the catalogue of trails in the state forests.
        var fullNames = new ArrayList<String>();
        placemarks(files.trailStarts()).forEach(start -> fullNames.add(start.attribute("LAYER")));
        catalogue(files.trailCatalogue(), TRAIL_NAME).forEach(row -> fullNames.add(row.get(TRAIL_NAME)));
        fullNames.removeIf(name -> name == null || name.isBlank());
        sites.addAll(trails(placemarks(files.trailRoutes()), fullNames));
        for (var kind : List.of("picnic", "camping", "trail")) {
            if (sites.stream().noneMatch(site -> site.kind().equals(kind))) {
                throw new IllegalArgumentException("No " + kind + " sites");
            }
        }
        if (sites.size() > MAX_SITES) throw new IllegalArgumentException("Too many sites");
        return new NatureSnapshot(retrievedAt, sites);
    }

    /** Several points of one picnic site share its name; the first point stands for the site. */
    private List<NatureSnapshot.Site> picnicSites(List<Placemark> placemarks) {
        var sites = new ArrayList<NatureSnapshot.Site>();
        var kept = new ArrayList<Placemark>();
        for (var placemark : placemarks) {
            var name = latin(placemark.attribute("LAYER"));
            if (name == null || placemark.lines().size() != 1) continue;
            var point = placemark.first();
            if (kept.stream().anyMatch(other -> name.equals(latin(other.attribute("LAYER")))
                    && metres(other.first(), point) < SAME_SITE_METRES)) continue;
            kept.add(placemark);
            sites.add(new NatureSnapshot.Site("picnic-" + placemark.id(), "picnic", name, placemark.attribute("Name"),
                    point[0], point[1], null, null, List.of()));
        }
        return sites;
    }

    private List<NatureSnapshot.Site> campingSites(List<Placemark> placemarks, List<Map<String, String>> catalogue) {
        var sites = new ArrayList<NatureSnapshot.Site>();
        for (var placemark : placemarks) {
            var name = latin(placemark.attribute("NameEN"));
            var greek = placemark.attribute("NamwGr");
            // Governor's Beach has the two names in each other's fields.
            if (name != null && GREEK.matcher(name).find() && (greek == null || !GREEK.matcher(greek).find())) {
                var swapped = name;
                name = latin(greek);
                greek = swapped;
            }
            if (name == null || placemark.lines().size() != 1) continue;
            Integer capacity = null;
            // The catalogue spells the names out ("Σταυρός" is "Σταυρός Ψώκας") and adds the capacity.
            if (greek != null) {
                var key = normalized(greek);
                var rows = catalogue.stream()
                        .filter(row -> normalized(row.getOrDefault(CAMPING_NAME, "")).startsWith(key)).toList();
                if (rows.size() == 1) {
                    greek = rows.getFirst().get(CAMPING_NAME);
                    capacity = positiveInteger(rows.getFirst().get("Max_Capacity_in_people"));
                } else if (greek.codePointCount(0, greek.length()) >= CAMPING_NAME_LIMIT) {
                    greek += "…";
                }
            }
            var point = placemark.first();
            sites.add(new NatureSnapshot.Site("camping-" + placemark.id(), "camping", name, greek, point[0], point[1],
                    null, capacity, List.of()));
        }
        return sites;
    }

    /** Route pieces with the same Greek name form one trail; its length is the sum of the published lengths. */
    private List<NatureSnapshot.Site> trails(List<Placemark> placemarks, List<String> fullNames) {
        var groups = new LinkedHashMap<String, List<Placemark>>();
        for (var placemark : placemarks) {
            var greek = placemark.attribute("Name_1");
            // Unnamed pieces ("ROAD5") are access roads, not trails.
            if (greek != null) groups.computeIfAbsent(greek, ignored -> new ArrayList<>()).add(placemark);
        }
        var trails = new ArrayList<NatureSnapshot.Site>();
        for (var group : groups.values()) {
            var first = group.getFirst();
            var name = latin(first.attribute("LAYER"));
            if (name == null) continue;
            var route = new ArrayList<List<double[]>>();
            double metres = 0;
            for (var piece : group) {
                for (var line : piece.lines()) if (line.size() > 1) route.add(simplify(line));
                metres += length(piece);
            }
            if (route.isEmpty()) continue;
            var start = route.getFirst().getFirst();
            trails.add(new NatureSnapshot.Site("trail-" + first.id(), "trail", name,
                    complete(first.attribute("Name_1"), fullNames), start[0], start[1],
                    Math.round(metres / 100) / 10.0, null, route));
        }
        return trails;
    }

    /** The published Shape_Leng in metres ("2564,76624"), or the measured line length when it is missing. */
    private static double length(Placemark piece) {
        var published = piece.attribute("Shape_Leng");
        if (published != null) {
            try {
                var value = Double.parseDouble(published.replace(',', '.'));
                if (Double.isFinite(value) && value > 0) return value;
            } catch (NumberFormatException ignored) {
                // measured below
            }
        }
        double metres = 0;
        for (var line : piece.lines()) for (int i = 1; i < line.size(); i++) metres += metres(line.get(i - 1), line.get(i));
        return metres;
    }

    private List<Placemark> placemarks(byte[] kml) {
        var root = reader.read(kml).getDocumentElement();
        if (!"kml".equals(root.getLocalName()) || !KML_NAMESPACE.equals(root.getNamespaceURI())) {
            throw new IllegalArgumentException("Not a KML document");
        }
        var nodes = root.getElementsByTagNameNS(KML_NAMESPACE, "Placemark");
        if (nodes.getLength() == 0 || nodes.getLength() > MAX_SITES * 10) {
            throw new IllegalArgumentException("Unexpected number of placemarks");
        }
        var placemarks = new ArrayList<Placemark>();
        for (int i = 0; i < nodes.getLength(); i++) {
            var element = (Element) nodes.item(i);
            var attributes = new HashMap<String, String>();
            var description = element.getElementsByTagNameNS(KML_NAMESPACE, "description");
            if (description.getLength() > 0) {
                var match = ATTRIBUTE.matcher(description.item(0).getTextContent());
                while (match.find()) attributes.putIfAbsent(match.group(1).strip(), match.group(2));
            }
            var lines = new ArrayList<List<double[]>>();
            var coordinates = element.getElementsByTagNameNS(KML_NAMESPACE, "coordinates");
            for (int c = 0; c < coordinates.getLength(); c++) {
                var line = line(coordinates.item(c).getTextContent());
                if (!line.isEmpty()) lines.add(line);
            }
            var id = attributes.getOrDefault("FID", element.getAttribute("id")).strip();
            if (!lines.isEmpty() && !id.isEmpty()) placemarks.add(new Placemark(id, attributes, lines));
        }
        return placemarks;
    }

    /** KML "lon,lat,alt lon,lat,alt …" as [latitude, longitude] pairs; points outside Cyprus are dropped. */
    private static List<double[]> line(String text) {
        var points = new ArrayList<double[]>();
        for (var tuple : text.strip().split("\\s+")) {
            var parts = tuple.split(",");
            if (parts.length < 2) continue;
            try {
                var longitude = Double.parseDouble(parts[0]);
                var latitude = Double.parseDouble(parts[1]);
                if (CyprusBounds.contains(latitude, longitude)) points.add(new double[] {round(latitude), round(longitude)});
            } catch (NumberFormatException ignored) {
                // a malformed tuple is skipped
            }
        }
        return points;
    }

    private static byte[] unzipKml(byte[] kmz) {
        try (var zip = new ZipInputStream(new ByteArrayInputStream(kmz))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (!entry.isDirectory() && entry.getName().toLowerCase(Locale.ROOT).endsWith(".kml")) {
                    var bytes = zip.readNBytes(MAX_FILE_BYTES + 1);
                    if (bytes.length > MAX_FILE_BYTES) throw new IllegalArgumentException("KML in KMZ is too large");
                    return bytes;
                }
            }
        } catch (IOException invalid) {
            throw new IllegalArgumentException("Invalid KMZ", invalid);
        }
        throw new IllegalArgumentException("KMZ without KML");
    }

    /** Rows of a small comma-separated file with a header line and optional double quotes. */
    private static List<Map<String, String>> catalogue(byte[] csv, String nameColumn) {
        var lines = new String(csv, StandardCharsets.UTF_8).replace("﻿", "").split("\\r?\\n");
        var header = fields(lines[0]);
        if (!header.contains(nameColumn)) throw new IllegalArgumentException("Catalogue without " + nameColumn);
        var rows = new ArrayList<Map<String, String>>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) continue;
            var values = fields(lines[i]);
            var row = new HashMap<String, String>();
            for (int c = 0; c < Math.min(header.size(), values.size()); c++) row.put(header.get(c), values.get(c).strip());
            rows.add(row);
        }
        return rows;
    }

    private static List<String> fields(String line) {
        var fields = new ArrayList<String>();
        var field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"' && quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                field.append('"');
                i++;
            } else if (c == '"') {
                quoted = !quoted;
            } else if (c == ',' && !quoted) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString());
        return fields;
    }

    /** A possibly cut Greek trail name, completed when exactly one fuller name starts with it, else marked "…". */
    static String complete(String name, List<String> fullNames) {
        if (name.getBytes(StandardCharsets.UTF_8).length < TRAIL_NAME_CUT_BYTES) return name;
        var key = normalized(name);
        if (fullNames.stream().anyMatch(full -> normalized(full).equals(key))) return name;
        var matches = new LinkedHashMap<String, String>();
        for (var full : fullNames) {
            if (normalized(full).startsWith(key)) matches.putIfAbsent(normalized(full), full.strip());
        }
        return matches.size() == 1 ? matches.values().iterator().next() : name + "…";
    }

    /** Lower case without accents, spaces or punctuation: "Κόννοι - Σπηλιά" and "Κοννοι-Σπηλια" are equal. */
    private static String normalized(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    /** "Mantra_Kambiou_kykliko" → "Mantra Kambiou kykliko". */
    private static String latin(String name) {
        if (name == null) return null;
        var spaced = SPACES.matcher(name).replaceAll(" ").strip();
        return spaced.isEmpty() ? null : spaced;
    }

    private static Integer positiveInteger(String value) {
        try {
            var number = Integer.parseInt(value == null ? "" : value.strip());
            return number > 0 ? number : null;
        } catch (NumberFormatException notNumber) {
            return null;
        }
    }

    /** Douglas–Peucker simplification of a [latitude, longitude] line. */
    static List<double[]> simplify(List<double[]> line) {
        if (line.size() < 3) return line;
        var keep = new boolean[line.size()];
        keep[0] = keep[line.size() - 1] = true;
        var stack = new ArrayList<int[]>();
        stack.add(new int[] {0, line.size() - 1});
        while (!stack.isEmpty()) {
            var range = stack.removeLast();
            double farthest = 0;
            int index = -1;
            for (int i = range[0] + 1; i < range[1]; i++) {
                var offset = offset(line.get(i), line.get(range[0]), line.get(range[1]));
                if (offset > farthest) {
                    farthest = offset;
                    index = i;
                }
            }
            if (index >= 0 && farthest > ROUTE_TOLERANCE_DEGREES) {
                keep[index] = true;
                stack.add(new int[] {range[0], index});
                stack.add(new int[] {index, range[1]});
            }
        }
        var simplified = new ArrayList<double[]>();
        for (int i = 0; i < line.size(); i++) if (keep[i]) simplified.add(line.get(i));
        return simplified;
    }

    /** Distance in degrees from a point to the segment between two others. */
    private static double offset(double[] point, double[] start, double[] end) {
        double dy = end[0] - start[0];
        double dx = end[1] - start[1];
        double squared = dx * dx + dy * dy;
        double t = squared == 0 ? 0 : Math.clamp(((point[1] - start[1]) * dx + (point[0] - start[0]) * dy) / squared, 0, 1);
        return Math.hypot(point[1] - (start[1] + t * dx), point[0] - (start[0] + t * dy));
    }

    /** Equirectangular distance, accurate to well under a percent at the size of Cyprus. */
    private static double metres(double[] from, double[] to) {
        double latitude = Math.toRadians((from[0] + to[0]) / 2);
        double x = Math.toRadians(to[1] - from[1]) * Math.cos(latitude);
        double y = Math.toRadians(to[0] - from[0]);
        return Math.hypot(x, y) * 6_371_000;
    }

    private static double round(double degrees) {
        return Math.round(degrees * 100_000) / 100_000.0;
    }
}
