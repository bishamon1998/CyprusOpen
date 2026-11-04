package com.cyprusopen.statistics;

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
class StatisticsApiIT {
    @LocalServerPort
    private int port;

    @Test
    void reportsUnimportedDatasetsWithoutInventingValues() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/statistics/datasets")).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
            var datasets = JsonMapper.builder().build().readTree(response.body());
            assertThat(datasets.size()).isEqualTo(2);
            assertThat(datasets.get(0).get("code").asText()).isEqualTo("immigration");
            assertThat(datasets.get(1).get("code").asText()).isEqualTo("tourism");
            for (var dataset : datasets) {
                assertThat(dataset.get("availability").asText()).isEqualTo("not_imported");
                assertThat(dataset.get("lastUpdatedAt").isNull()).isTrue();
            }
        }
    }
}
