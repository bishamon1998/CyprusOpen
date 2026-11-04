package com.cyprusopen.parking;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
    void hasNoAvailabilityBeforeTheFirstVerifiedReport() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/parking")).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
            var report = JsonMapper.builder().build().readTree(response.body());
            assertThat(report.get("freshness").asText()).isEqualTo("unavailable");
            assertThat(report.get("lastUpdatedAt").isNull()).isTrue();
            assertThat(report.get("items").size()).isZero();
        }
    }
}
