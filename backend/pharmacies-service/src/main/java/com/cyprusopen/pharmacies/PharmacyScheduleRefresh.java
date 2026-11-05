package com.cyprusopen.pharmacies;

import static com.cyprusopen.pharmacies.PharmacyScheduleConstants.CYPRUS;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "pharmacies.sync-enabled", havingValue = "true")
class PharmacyScheduleRefresh {
    private static final Logger LOG = LoggerFactory.getLogger(PharmacyScheduleRefresh.class);
    private static final URI METADATA = URI.create("https://www.data.gov.cy/el/dataset/817/dcat-ap-2.0/json");
    private static final Map<String, String> DISTRICTS = Map.of(
            "\u039b\u03b5\u03c5\u03ba\u03c9\u03c3\u03af\u03b1", "nicosia",
            "\u039b\u03ac\u03c1\u03bd\u03b1\u03ba\u03b1", "larnaca",
            "\u039b\u03b5\u03bc\u03b5\u03c3\u03cc\u03c2", "limassol",
            "\u03a0\u03ac\u03c6\u03bf\u03c2", "paphos",
            "\u0391\u03bc\u03bc\u03cc\u03c7\u03c9\u03c3\u03c4\u03bf\u03c2", "famagusta");
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final PharmacySchedules schedules;
    private final PharmacyCsvReader reader;
    private final JsonMapper mapper;
    private final Path snapshotFile;

    @Autowired
    PharmacyScheduleRefresh(PharmacySchedules schedules, PharmacyCsvReader reader, JsonMapper mapper,
            @Value("${pharmacies.snapshot-file}") Path snapshotFile) throws IOException {
        this.schedules = schedules;
        this.reader = reader;
        this.mapper = mapper;
        this.snapshotFile = snapshotFile.toAbsolutePath();
        if (Files.exists(this.snapshotFile)) {
            schedules.replace(mapper.readValue(Files.readString(this.snapshotFile), PharmacySchedules.Snapshot.class));
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    void initialize() {
        var checkedAt = schedules.snapshot().checkedAt();
        if (checkedAt == null || checkedAt.isBefore(Instant.now().minus(Duration.ofDays(1)))) refresh();
    }

    @Scheduled(cron = "0 15 6 * * *", zone = "Asia/Nicosia")
    synchronized void refresh() {
        try {
            var metadata = mapper.readTree(download(METADATA));
            var entries = new ArrayList<PharmacyCsvReader.DutyEntry>();
            var loaded = new HashSet<String>();
            var today = LocalDate.now(CYPRUS);
            var previous = schedules.snapshot().entries();
            for (var distribution : metadata.path("dcat:Distribution")) {
                var district = DISTRICTS.get(distribution.path("dct:title").asText().strip());
                if (district == null) continue;
                if (!loaded.add(district)) throw new IOException("Duplicate district: " + district);
                var url = URI.create(distribution.path("dcat:downloadURL").path("@rdf:resource").asText());
                var districtEntries = reader.read(new StringReader(download(url)), district);
                if (districtEntries.stream().noneMatch(entry -> !entry.date().isBefore(today))) {
                    throw new IOException("No current or upcoming entries for " + district);
                }
                var firstNewDay = districtEntries.stream().map(PharmacyCsvReader.DutyEntry::date)
                        .min(Comparator.naturalOrder()).orElseThrow();
                previous.stream().filter(entry -> entry.district().equals(district)
                        && !entry.date().isBefore(today.minusDays(1)) && entry.date().isBefore(firstNewDay))
                        .forEach(entries::add);
                entries.addAll(districtEntries);
            }
            if (loaded.size() != DISTRICTS.size()) throw new IOException("Missing district schedules");
            var next = new PharmacySchedules.Snapshot(Instant.now(), List.copyOf(entries));
            persist(next);
            schedules.replace(next);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            LOG.warn("Pharmacy schedule check interrupted");
        } catch (IOException | RuntimeException error) {
            LOG.warn("Pharmacy schedule check failed. Keeping the previous schedule.", error);
        }
    }

    private void persist(PharmacySchedules.Snapshot next) throws IOException {
        var directory = Files.createDirectories(snapshotFile.getParent());
        var temporary = Files.createTempFile(directory, "pharmacy-schedules-", ".tmp");
        try {
            Files.writeString(temporary, mapper.writeValueAsString(next));
            Files.move(temporary, snapshotFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private String download(URI uri) throws IOException, InterruptedException {
        if (!"https".equals(uri.getScheme()) || !"www.data.gov.cy".equals(uri.getHost())) {
            throw new IOException("Unexpected pharmacy schedule origin");
        }
        var request = HttpRequest.newBuilder(uri).timeout(TIMEOUT)
                .header("User-Agent", "CyprusOpen Pharmacy Schedule").GET().build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (var input = response.body()) {
            if (response.statusCode() != 200) throw new IOException("Source returned " + response.statusCode());
            var body = input.readNBytes(MAX_BYTES + 1);
            if (body.length > MAX_BYTES) throw new IOException("Schedule response exceeds size limit");
            return new String(body, StandardCharsets.UTF_8);
        }
    }
}
