package com.cyprusopen.places.beach;

import static org.assertj.core.api.Assertions.assertThat;

import com.cyprusopen.common.refresh.RefreshResult;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Imports trimmed copies of the real EEA Discodata tables (three current Cyprus bathing waters and one dropped
 * from the designation) and serves them.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class BeachApiIT {
    static final AtomicReference<String> mode = new AtomicReference<>("ok");
    static final AtomicReference<String> lastQuery = new AtomicReference<>();
    static final HttpServer upstream;

    static {
        try {
            upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            upstream.createContext("/sql", exchange -> {
                var query = URLDecoder.decode(exchange.getRequestURI().getRawQuery(), StandardCharsets.UTF_8);
                lastQuery.set(query);
                var fixture = query.contains("[spatial_ProtectedArea]") ? "beach-spatial.json"
                        : query.contains("[assessment_BathingWaterStatus]") ? "beach-status.json"
                        : query.contains("[assessment_BathingWaterQuality]") ? "beach-quality.json" : null;
                if (mode.get().equals("unavailable") || fixture == null) {
                    exchange.sendResponseHeaders(403, -1);
                } else {
                    byte[] body;
                    if (mode.get().equals("invalid")) {
                        body = "{\"errors\":[{\"error\":\"Invalid object name\"}]}".getBytes(StandardCharsets.UTF_8);
                    } else {
                        try (var input = BeachApiIT.class.getResourceAsStream("/" + fixture)) {
                            body = input.readAllBytes();
                        }
                    }
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                }
                exchange.close();
            });
            upstream.start();
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-bookworm")
            .withDatabaseName("parking").withUsername("parking_app").withPassword("integration-test-only")
            .withInitScript("parking-schema.sql");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("beaches.discodata-url", () -> "http://127.0.0.1:" + upstream.getAddress().getPort() + "/sql");
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl()
                + (postgres.getJdbcUrl().contains("?") ? "&" : "?") + "currentSchema=parking");
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JsonMapper mapper;

    @Autowired
    private BeachSync sync;

    @Autowired
    private JdbcClient jdbc;

    @AfterAll
    static void stopUpstream() {
        upstream.stop(0);
    }

    @BeforeEach
    void emptyStoreAndHealthySource() {
        jdbc.sql("DELETE FROM beaches").update();
        mode.set("ok");
    }

    /** Joins the three EEA tables; only the current designation is served, with the latest class and samples. */
    @Test
    void importsCurrentBathingWatersWithTheirClasses() throws Exception {
        assertThat(get().get("beaches").size()).isZero();
        assertThat(sync.needsInitialRun()).isTrue();

        assertThat(sync.refresh()).isEqualTo(RefreshResult.UPDATED);
        // Requests stay limited to the Cyprus rows of fixed tables.
        assertThat(lastQuery.get()).contains("WHERE countryCode='CY'");
        assertThat(sync.needsInitialRun()).isFalse();
        var imported = get();
        assertThat(imported.get("season").asInt()).isEqualTo(2024);
        // VALOU was last designated in 2008 and is left out; the rest are sorted by name.
        assertThat(imported.get("beaches").findValuesAsString("name")).containsExactly("KANALI POMOU", "KARAFA", "POTAMI");

        var karafa = beach(imported, "CY0001457000000154");
        assertThat(karafa.get("quality").asString()).isEqualTo("excellent");
        assertThat(karafa.get("samples").asInt()).isEqualTo(28);
        assertThat(karafa.get("escherichiaColi").asDouble()).isBetween(24.4, 24.5);
        assertThat(karafa.get("history").size()).isEqualTo(10);
        assertThat(karafa.get("history").get(0).get("season").asInt()).isEqualTo(2024);
    }

    @Test
    void sourceFailuresKeepTheLastBathingWaters() throws Exception {
        assertThat(sync.refresh()).isEqualTo(RefreshResult.UPDATED);
        var stored = get();
        mode.set("invalid");
        assertThat(sync.refresh()).isEqualTo(RefreshResult.INVALID);
        mode.set("unavailable");
        assertThat(sync.refresh()).isEqualTo(RefreshResult.UNAVAILABLE);
        assertThat(get()).isEqualTo(stored);
    }

    private JsonNode get() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/beaches"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            return mapper.readTree(response.body());
        }
    }

    private static JsonNode beach(JsonNode response, String id) {
        for (var beach : response.get("beaches")) {
            if (beach.get("id").asString().equals(id)) return beach;
        }
        return null;
    }
}
