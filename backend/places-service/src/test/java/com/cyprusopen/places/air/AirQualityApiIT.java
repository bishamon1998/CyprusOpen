package com.cyprusopen.places.air;

import static org.assertj.core.api.Assertions.assertThat;

import com.cyprusopen.common.refresh.RefreshResult;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
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

/** Imports a trimmed copy of the real source response and serves it with European Air Quality Index levels. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AirQualityApiIT {
    static final AtomicReference<String> mode = new AtomicReference<>("ok");
    static final HttpServer upstream;

    static {
        try {
            upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            upstream.createContext("/air", exchange -> {
                if (mode.get().equals("unavailable")) {
                    exchange.sendResponseHeaders(503, -1);
                } else {
                    byte[] body;
                    if (mode.get().equals("invalid")) {
                        body = "{\"data\":{}}".getBytes(StandardCharsets.UTF_8);
                    } else {
                        try (var input = AirQualityApiIT.class.getResourceAsStream("/air-quality.json")) {
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
        registry.add("air-quality.resource-url", () -> "http://127.0.0.1:" + upstream.getAddress().getPort() + "/air");
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
    private AirQualitySync sync;

    @Autowired
    private JdbcClient jdbc;

    @AfterAll
    static void stopUpstream() {
        upstream.stop(0);
    }

    @BeforeEach
    void emptyStoreAndHealthySource() {
        jdbc.sql("DELETE FROM air_quality").update();
        mode.set("ok");
    }

    /** Levels follow the European Air Quality Index bands; a station placed outside Cyprus is dropped. */
    @Test
    void importsStationsWithIndexLevels() throws Exception {
        assertThat(get().get("stations").size()).isZero();
        assertThat(sync.needsInitialRun()).isTrue();

        assertThat(sync.refresh()).isEqualTo(RefreshResult.UPDATED);
        assertThat(sync.needsInitialRun()).isFalse();
        var imported = get();
        assertThat(imported.get("stations").size()).isEqualTo(3);

        var nicosia = station(imported, "station_1");
        assertThat(nicosia.get("name").get("el").asString()).startsWith("Λευκωσία");
        assertThat(nicosia.get("measuredAt").asString()).isEqualTo("2026-11-07T17:00:00Z");
        // NO2 55 µg/m³ is "moderate" (26-60) and the worst pollutant at this station.
        assertThat(nicosia.get("level").asString()).isEqualTo("moderate");
        assertThat(reading(nicosia, "pm25").get("level").asString()).isEqualTo("fair");
        assertThat(reading(nicosia, "co").get("level").isNull()).isTrue();
        assertThat(reading(nicosia, "nox")).isNull();
        // O3 80 µg/m³ is "fair" (61-100).
        assertThat(station(imported, "station_10").get("level").asString()).isEqualTo("fair");
    }

    @Test
    void sourceFailuresKeepTheLastReadings() throws Exception {
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
            var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/air-quality"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Cache-Control")).hasValue("no-store");
            return mapper.readTree(response.body());
        }
    }

    private static JsonNode station(JsonNode response, String id) {
        for (var station : response.get("stations")) {
            if (station.get("id").asString().equals(id)) return station;
        }
        return null;
    }

    private static JsonNode reading(JsonNode station, String pollutant) {
        for (var reading : station.get("readings")) {
            if (reading.get("pollutant").asString().equals(pollutant)) return reading;
        }
        return null;
    }
}
