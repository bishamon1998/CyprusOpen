package com.cyprusopen.parking.beach;

import static com.cyprusopen.parking.beach.BeachConstants.HISTORY_SEASONS;
import static com.cyprusopen.parking.beach.BeachConstants.MAX_BEACHES;

import com.cyprusopen.common.geo.CyprusBounds;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Joins the three Discodata tables by bathing water identifier. Only bathing waters of the latest reporting year
 * (the current designation) with coordinates in Cyprus are kept. Throws {@link IllegalArgumentException} when a
 * table is not a Discodata result or no bathing water remains.
 */
@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class BeachParser {
    /** "1 - Excellent" → "excellent", "0 - Not classified" → "not-classified". */
    private static final Pattern QUALITY = Pattern.compile("\\d+\\s*-\\s*(.+)");
    private final JsonMapper mapper;

    BeachSnapshot parse(byte[] spatial, byte[] status, byte[] quality, Instant retrievedAt) {
        var places = results(spatial);
        var currentYear = 0;
        for (var place : places) currentYear = Math.max(currentYear, place.path("cYear").asInt(0));

        var history = new HashMap<String, List<BeachSnapshot.Classification>>();
        for (var row : results(status)) {
            var classification = quality(row.path("quality").asString(""));
            var season = row.path("season").asInt(0);
            if (classification.isEmpty() || season == 0) continue;
            history.computeIfAbsent(row.path("bathingWaterIdentifier").asString(""), id -> new ArrayList<>())
                    .add(new BeachSnapshot.Classification(season, classification.get()));
        }
        var samples = new HashMap<String, JsonNode>();
        for (var row : results(quality)) {
            samples.merge(row.path("bathingWaterIdentifier").asString(""), row,
                    (kept, next) -> next.path("season").asInt(0) > kept.path("season").asInt(0) ? next : kept);
        }

        var beaches = new ArrayList<BeachSnapshot.Beach>();
        Integer latestSeason = null;
        for (var place : places) {
            if (place.path("cYear").asInt(0) != currentYear) continue;
            var id = place.path("thematicIdIdentifier").asString("").strip();
            var name = place.path("nameTextInternational").asString(place.path("nameText").asString("")).strip();
            var latitude = place.path("lat").asDouble(Double.NaN);
            var longitude = place.path("lon").asDouble(Double.NaN);
            if (id.isEmpty() || name.isEmpty() || !CyprusBounds.contains(latitude, longitude)) continue;
            if (beaches.size() == MAX_BEACHES) throw new IllegalArgumentException("Too many bathing waters");

            var seasons = history.getOrDefault(id, List.of()).stream()
                    .sorted(Comparator.comparingInt(BeachSnapshot.Classification::season).reversed())
                    .limit(HISTORY_SEASONS).toList();
            var latest = seasons.isEmpty() ? null : seasons.getFirst();
            var sample = samples.get(id);
            boolean sampledLatest = sample != null && latest != null && sample.path("season").asInt(0) == latest.season();
            beaches.add(new BeachSnapshot.Beach(id, name, latitude, longitude, profile(place.path("link").asString("")),
                    latest == null ? null : latest.quality(), latest == null ? null : latest.season(), seasons,
                    sampledLatest ? positive(sample.path("escherichiaColiNumberOfSamples")).map(Double::intValue).orElse(null) : null,
                    sampledLatest ? positive(sample.path("escherichiaColi95thPercentile")).orElse(null) : null,
                    sampledLatest ? positive(sample.path("intestinalEnterococci95thPercentile")).orElse(null) : null));
            if (latest != null && (latestSeason == null || latest.season() > latestSeason)) latestSeason = latest.season();
        }
        if (beaches.isEmpty()) throw new IllegalArgumentException("No bathing waters in Cyprus");
        beaches.sort(Comparator.comparing(BeachSnapshot.Beach::name));
        return new BeachSnapshot(retrievedAt, latestSeason, beaches);
    }

    private List<JsonNode> results(byte[] body) {
        JsonNode root;
        try {
            root = mapper.readTree(body);
        } catch (JacksonException invalid) {
            throw new IllegalArgumentException("Discodata response is not JSON", invalid);
        }
        var results = root.path("results");
        if (!results.isArray()) throw new IllegalArgumentException("Discodata response has no results");
        var rows = new ArrayList<JsonNode>();
        results.forEach(rows::add);
        return rows;
    }

    static Optional<String> quality(String label) {
        var match = QUALITY.matcher(label.strip());
        if (!match.matches()) return Optional.empty();
        return Optional.of(match.group(1).strip().toLowerCase(Locale.ROOT).replaceAll("[^a-z]+", "-"));
    }

    /** The bathing water profile document; only http(s) links are passed on. */
    private static String profile(String link) {
        var trimmed = link.strip();
        return trimmed.startsWith("https://") || trimmed.startsWith("http://") ? trimmed : null;
    }

    private static Optional<Double> positive(JsonNode node) {
        if (!node.isNumber()) return Optional.empty();
        var value = node.asDouble();
        return Double.isFinite(value) && value >= 0 ? Optional.of(value) : Optional.empty();
    }
}
