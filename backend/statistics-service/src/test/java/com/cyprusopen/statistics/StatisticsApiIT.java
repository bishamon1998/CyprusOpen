package com.cyprusopen.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import com.cyprusopen.common.refresh.RefreshResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class StatisticsApiIT {
    /** Table path below the API root to fixture file, shared with the browser CI fixture server. */
    static final Map<String, String> FIXTURES = fixtures();
    static final AtomicReference<String> mode = new AtomicReference<>("initial");
    /** Answers the next request with 429 Too Many Requests, as CYSTAT does for bursts. */
    static final AtomicBoolean throttleNext = new AtomicBoolean();
    static final HttpServer upstream;

    static {
        try {
            upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            upstream.createContext("/api/", StatisticsApiIT::table);
            upstream.start();
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-bookworm")
            .withDatabaseName("statistics").withUsername("statistics_app").withPassword("integration-test-only")
            .withInitScript("statistics-schema.sql");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("statistics.cystat-api-url",
                () -> "http://127.0.0.1:" + upstream.getAddress().getPort() + "/api/");
        registry.add("statistics.cystat-request-interval", () -> "0s");
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl()
                + (postgres.getJdbcUrl().contains("?") ? "&" : "?") + "currentSchema=statistics");
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JsonMapper mapper;

    @Autowired
    private StatisticsRefresh refresh;

    @Autowired
    private List<StatisticsDataset> datasets;

    @AfterAll
    static void stopUpstream() {
        upstream.stop(0);
    }

    @Test
    void importsCystatTablesAndKeepsThemOnSourceErrors() throws Exception {
        var sources = get("/api/v1/statistics/datasets", 200);
        assertThat(sources.size()).isEqualTo(datasets.size()).isEqualTo(FIXTURES.size());
        assertThat(sources.get(0).get("code").asString()).isEqualTo("immigrant-groups");
        assertThat(sources.get(0).get("updatedAt").isNull()).isTrue();
        assertThat(get("/api/v1/statistics/datasets/tourist-arrivals", 200).get("series").size()).isZero();

        var imported = refresh.refreshAll();
        assertThat(imported).hasSize(datasets.size())
                .allSatisfy((code, result) -> assertThat(result).as(code).isEqualTo(RefreshResult.UPDATED));
        for (var dataset : datasets) {
            assertThat(get("/api/v1/statistics/datasets/" + dataset.code(), 200).get("series").size())
                    .as(dataset.code()).isPositive();
        }

        var tourism = get("/api/v1/statistics/datasets/tourist-arrivals", 200);
        assertThat(tourism.get("source").get("updatedAt").asString()).isEqualTo("2026-10-19T05:00:00Z");
        assertThat(tourism.get("source").get("url").asString()).isEqualTo("https://www.data.gov.cy/en/dataset/2285");
        assertThat(mapper.convertValue(tourism.get("periods"), String[].class))
                .containsExactly("2025-12", "2026-01", "2026-02");
        assertThat(line(tourism, "EUROPE")).isNull();
        assertThat(line(tourism, "total").get("values").toString()).isEqualTo("[1000.0,1100.0,1200.0]");
        assertThat(line(tourism, "GB").get("values").toString()).isEqualTo("[400.0,420.0,null]");
        assertThat(line(tourism, "GE").get("values").toString()).isEqualTo("[15.0,7.0,9.0]");
        assertThat(line(tourism, "RU").get("lowReliability").toString()).isEqualTo("[0]");

        var immigrants = get("/api/v1/statistics/datasets/immigrant-groups", 200);
        assertThat(line(immigrants, "total").get("values").toString()).isEqualTo("[30000.0,31000.0]");
        assertThat(line(immigrants, "citizenship-eu").get("values").toString()).isEqualTo("[7000.0,7200.0]");
        assertThat(line(immigrants, "previous-residence-non-eu").get("values").toString())
                .isEqualTo("[9000.0,9100.0]");
        var movements = get("/api/v1/statistics/datasets/migration-movements", 200);
        assertThat(line(movements, "emigrants").get("values").toString()).isEqualTo("[null,18000.0]");
        get("/api/v1/statistics/datasets/unknown", 404);

        // A table without a series dimension, one with two series dimensions and pinned dimensions.
        var inflation = get("/api/v1/statistics/datasets/inflation", 200);
        assertThat(inflation.get("series").size()).isEqualTo(1);
        assertThat(line(inflation, "inflation").get("values").size()).isEqualTo(3);
        var earnings = get("/api/v1/statistics/datasets/earnings", 200);
        assertThat(line(earnings, "median-total")).isNotNull();
        assertThat(line(earnings, "mean-women")).isNotNull();
        var fuel = get("/api/v1/statistics/datasets/fuel-sales", 200);
        assertThat(line(fuel, "diesel")).isNotNull();
        assertThat(line(fuel, "lpg")).isNotNull();
        assertThat(get("/api/v1/statistics/datasets/price-changes", 200).get("periods").size()).isEqualTo(2);

        // Calendar tables: one series per date; dates that do not exist (30 February) are null and left out.
        var births = get("/api/v1/statistics/datasets/birth-dates", 200);
        assertThat(mapper.convertValue(births.get("periods"), String[].class)).containsExactly("2023", "2024");
        assertThat(births.get("series").size()).isEqualTo(366);
        assertThat(line(births, "02-29").get("values").toString()).isEqualTo("[null,14.0]");
        assertThat(line(births, "02-30")).isNull();
        assertThat(line(births, "12-25").get("values").toString()).isEqualTo("[9.0,13.0]");
        var weddings = get("/api/v1/statistics/datasets/wedding-dates", 200);
        assertThat(line(weddings, "12-25").get("values").toString()).isEqualTo("[0.0,0.0]");
        assertThat(line(weddings, "total")).isNull();

        assertThat(refresh.refreshAll()).containsValue(RefreshResult.UNCHANGED).doesNotContainValue(RefreshResult.UPDATED);
        mode.set("invalid");
        assertThat(refresh.refreshAll()).hasSize(datasets.size())
                .allSatisfy((code, result) -> assertThat(result).isEqualTo(RefreshResult.INVALID));
        mode.set("unavailable");
        assertThat(refresh.refreshAll()).allSatisfy((code, result) -> assertThat(result).isEqualTo(RefreshResult.UNAVAILABLE));
        assertThat(line(get("/api/v1/statistics/datasets/tourist-arrivals", 200), "GB")).isNotNull();

        mode.set("revised");
        throttleNext.set(true);
        var tourist = datasets.stream().filter(dataset -> dataset.code().equals("tourist-arrivals")).findFirst().orElseThrow();
        assertThat(refresh.refresh(tourist)).isEqualTo(RefreshResult.UPDATED);
        var revised = get("/api/v1/statistics/datasets/tourist-arrivals", 200);
        assertThat(revised.get("source").get("updatedAt").asString()).isEqualTo("2026-11-19T05:00:00Z");
        assertThat(line(revised, "total").get("values").toString()).isEqualTo("[1000.0,1100.0,1250.0]");
    }

    private static Map<String, String> fixtures() {
        var fixtures = new HashMap<String, String>();
        try (var reader = new BufferedReader(new InputStreamReader(
                StatisticsApiIT.class.getResourceAsStream("/cystat/tables.properties"), StandardCharsets.UTF_8))) {
            reader.lines().filter(line -> !line.isBlank() && !line.startsWith("#")).forEach(line -> {
                var separator = line.indexOf('=');
                fixtures.put("/api/" + line.substring(0, separator), line.substring(separator + 1));
            });
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
        return fixtures;
    }

    private JsonNode get(String path, int status) throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(status);
            return mapper.readTree(response.body());
        }
    }

    private static JsonNode line(JsonNode dataset, String key) {
        for (var line : dataset.get("series")) {
            if (line.get("key").asString().equals(key)) return line;
        }
        return null;
    }

    private static void table(HttpExchange exchange) throws IOException {
        var query = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        var fixture = FIXTURES.get(exchange.getRequestURI().getPath());
        if (throttleNext.getAndSet(false)) {
            exchange.sendResponseHeaders(429, -1);
            exchange.close();
            return;
        }
        if (!"POST".equals(exchange.getRequestMethod()) || !query.contains("json-stat2") || fixture == null
                || mode.get().equals("unavailable")) {
            exchange.sendResponseHeaders(mode.get().equals("unavailable") ? 503 : 404, -1);
            exchange.close();
            return;
        }
        String body;
        try (var input = StatisticsApiIT.class.getResourceAsStream("/cystat/" + fixture)) {
            body = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        body = switch (mode.get()) {
            case "invalid" -> "{\"version\":\"2.0\",\"class\":\"dataset\",\"updated\":\"2026-12-01T00:00:00Z\"}";
            case "revised" -> body.replace("2026-10-19T05:00:00Z", "2026-11-19T05:00:00Z").replace("1000, 1100, 1200,", "1000, 1100, 1250,");
            default -> body;
        };
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
