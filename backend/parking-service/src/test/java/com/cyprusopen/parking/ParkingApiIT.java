package com.cyprusopen.parking;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ParkingApiIT {
    @LocalServerPort
    private int port;

    @Test
    void servesTheReviewedDirectoryWithoutClaimingLiveAvailability() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/parking")).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
            var report = JsonMapper.builder().build().readTree(response.body());
            assertThat(report.get("freshness").asText()).isEqualTo("unavailable");
            assertThat(report.get("lastUpdatedAt").isNull()).isTrue();
            assertThat(report.get("directory").get("license").asText()).isEqualTo("CC BY 4.0");
            assertThat(report.get("directory").get("publishedAt").asText()).startsWith("2026-11-03T");
            assertThat(report.get("items").size()).isEqualTo(651);
            var identifiers = new HashSet<String>();
            var nicosia = false;
            var limassol = false;
            var larnaca = false;
            var paphos = false;
            for (var item : report.get("items")) {
                assertThat(identifiers.add(item.get("id").asText())).isTrue();
                assertThat(item.get("availableSpaces").isNull()).isTrue();
                assertThat(item.get("name").asText()).isNotBlank().matches("[\\x20-\\x7E]+");
                var lat = item.get("latitude").asDouble();
                var lon = item.get("longitude").asDouble();
                assertThat(lat).isBetween(34.0, 36.0);
                assertThat(lon).isBetween(32.0, 35.0);
                nicosia |= lat > 35.04 && lat < 35.24 && lon > 33.21 && lon < 33.49;
                limassol |= lat > 34.61 && lat < 34.78 && lon > 32.9 && lon < 33.2;
                larnaca |= lat > 34.84 && lat < 35.01 && lon > 33.51 && lon < 33.72;
                paphos |= lat > 34.7 && lat < 34.87 && lon > 32.35 && lon < 32.52;
            }
            assertThat(nicosia && limassol && larnaca && paphos).isTrue();
        }
    }
}
