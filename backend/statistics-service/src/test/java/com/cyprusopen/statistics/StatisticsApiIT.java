package com.cyprusopen.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import com.cyprusopen.common.refresh.RefreshResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class StatisticsApiIT {
    /** Table path below the API root to fixture file, shared with the browser CI fixture server. */
    static final Map<String, String> FIXTURES = fixtures();
    /** How the fixture server answers every table. */
    static final AtomicReference<String> mode = new AtomicReference<>("initial");
    /** Overrides {@link #mode} for single tables, by fixture file name. */
    static final Map<String, String> faults = new ConcurrentHashMap<>();
    /** Answers the next request with 429 Too Many Requests, as CYSTAT does for bursts. */
    static final AtomicBoolean throttleNext = new AtomicBoolean();
    static final AtomicInteger redirectTargetHits = new AtomicInteger();
    static final HttpServer upstream;

    static {
        try {
            upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            upstream.createContext("/api/", StatisticsApiIT::table);
            upstream.createContext("/elsewhere", exchange -> {
                redirectTargetHits.incrementAndGet();
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
            });
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
        registry.add("statistics.datasource.url", () -> postgres.getJdbcUrl()
                + (postgres.getJdbcUrl().contains("?") ? "&" : "?") + "currentSchema=statistics");
        registry.add("statistics.datasource.username", postgres::getUsername);
        registry.add("statistics.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JsonMapper mapper;

    @Autowired
    private StatisticsRefresh refresh;

    @Autowired
    private List<StatisticsDataset> datasets;

    @Autowired
    private JdbcClient jdbc;

    @AfterAll
    static void stopUpstream() {
        upstream.stop(0);
    }

    @BeforeEach
    void emptyDatabaseAndHealthySource() {
        jdbc.sql("DELETE FROM dataset").update();
        mode.set("initial");
        faults.clear();
        throttleNext.set(false);
        redirectTargetHits.set(0);
    }

    @Test
    void importsEveryTableAndServesItsSeries() throws Exception {
        importAll();
        assertThat(refresh.refreshAll()).containsValue(RefreshResult.UNCHANGED).doesNotContainValue(RefreshResult.UPDATED);
        for (var dataset : datasets) {
            assertThat(get("/api/v1/statistics/datasets/" + dataset.code(), 200).get("series").size())
                    .as(dataset.code()).isPositive();
        }
        get("/api/v1/statistics/datasets/unknown", 404);

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

        // Quarterly periods, revision markers ("2023 (r)") and tables without a time dimension.
        var housePrices = get("/api/v1/statistics/datasets/house-prices", 200);
        assertThat(mapper.convertValue(housePrices.get("periods"), String[].class))
                .containsExactly("2025-Q2", "2025-Q3", "2025-Q4", "2026-Q1", "2026-Q2");
        assertThat(line(housePrices, "index").get("values").toString()).isEqualTo("[97.74,101.35,101.34,102.95,105.46]");
        var causes = get("/api/v1/statistics/datasets/death-causes", 200);
        assertThat(mapper.convertValue(causes.get("periods"), String[].class)).containsExactly("2022", "2023", "2024");
        assertThat(line(causes, "c2").get("values").toString()).isEqualTo("[1574.0,1606.0,1742.0]");
        assertThat(line(causes, "c2.1")).isNull();
        assertThat(causes.get("series").size()).isEqualTo(19);
        var spending = get("/api/v1/statistics/datasets/household-spending", 200);
        assertThat(mapper.convertValue(spending.get("periods"), String[].class)).containsExactly("2023");
        assertThat(spending.get("series").size()).isEqualTo(14);
        assertThat(line(spending, "food").get("values").toString()).isEqualTo("[1446.0]");
        assertThat(line(spending, "education").get("values").toString()).isEqualTo("[369.0]");
        var income = get("/api/v1/statistics/datasets/household-income", 200);
        assertThat(line(income, "district-lemesos").get("values").toString()).isEqualTo("[56147.0]");
        assertThat(line(income, "source-pensions").get("values").toString()).isEqualTo("[6911.0]");

        // Two series dimensions: nationality pairs and per-country measures (CYSTAT's "USA" is US).
        var pairs = get("/api/v1/statistics/datasets/marriage-pairs", 200);
        assertThat(line(pairs, "CY|RO").get("values").toString()).isEqualTo("[68.0,59.0]");
        assertThat(line(pairs, "other|other")).isNotNull();
        var tourists = get("/api/v1/statistics/datasets/tourist-spending", 200);
        assertThat(line(tourists, "day-US").get("values").get(1).asDouble()).isEqualTo(89.53);
        assertThat(line(tourists, "day-total").get("values").get(1).asDouble()).isEqualTo(98.21);
        assertThat(line(get("/api/v1/statistics/datasets/divorce-duration", 200), "10–14").get("values").toString())
                .isEqualTo("[394.0,359.0]");
        assertThat(line(get("/api/v1/statistics/datasets/vehicles", 200), "new-electric").get("values").toString())
                .isEqualTo("[1322.0,2220.0,2223.0]");
        assertThat(line(get("/api/v1/statistics/datasets/building-permits", 200), "total").get("values").get(13).asDouble())
                .isEqualTo(2122.0);
        assertThat(line(get("/api/v1/statistics/datasets/births-by-month", 200), "08").get("values").get(1).asDouble())
                .isEqualTo(839.0);
    }

    /** The page polls an empty table until the first import lands; a cached empty answer would hide it for an hour. */
    @Test
    void emptyTableIsNeverCached() throws Exception {
        var sources = get("/api/v1/statistics/datasets", 200);
        assertThat(sources.size()).isEqualTo(datasets.size()).isEqualTo(FIXTURES.size());
        assertThat(sources.get(0).get("updatedAt").isNull()).isTrue();
        assertThat(get("/api/v1/statistics/datasets/tourist-arrivals", 200).get("series").size()).isZero();
        assertThat(cacheControl("/api/v1/statistics/datasets/tourist-arrivals")).isEqualTo("no-store");

        importAll();
        assertThat(cacheControl("/api/v1/statistics/datasets/tourist-arrivals")).isEqualTo("max-age=3600, public");
        assertThat(cacheControl("/api/v1/statistics/datasets")).isEqualTo("no-store");
    }

    @Test
    void brokenTableDoesNotStopTheOthers() throws Exception {
        faults.put("tourist-arrivals.json", "malformed");
        faults.put("inflation.json", "unavailable");

        var results = refresh.refreshAll();
        assertThat(results).hasSize(datasets.size())
                .containsEntry("tourist-arrivals", RefreshResult.INVALID)
                .containsEntry("inflation", RefreshResult.UNAVAILABLE);
        results.forEach((code, result) -> {
            if (!code.equals("tourist-arrivals") && !code.equals("inflation")) {
                assertThat(result).as(code).isEqualTo(RefreshResult.UPDATED);
            }
        });
        assertThat(get("/api/v1/statistics/datasets/tourist-arrivals", 200).get("series").size()).isZero();
        assertThat(get("/api/v1/statistics/datasets/population", 200).get("series").size()).isPositive();
    }

    /** Invalid, unreachable, redirecting or oversized answers never replace stored tables. */
    @Test
    void sourceFailuresKeepTheStoredTables() throws Exception {
        importAll();
        for (var failure : Map.of("invalid", RefreshResult.INVALID, "unavailable", RefreshResult.UNAVAILABLE,
                "redirect", RefreshResult.UNAVAILABLE).entrySet()) {
            mode.set(failure.getKey());
            assertThat(refresh.refreshAll()).as(failure.getKey()).hasSize(datasets.size())
                    .allSatisfy((code, result) -> assertThat(result).isEqualTo(failure.getValue()));
        }
        assertThat(redirectTargetHits).hasValue(0);

        mode.set("initial");
        faults.put("tourist-arrivals.json", "oversized");
        assertThat(refresh.refresh(dataset("tourist-arrivals"))).isEqualTo(RefreshResult.UNAVAILABLE);

        var kept = get("/api/v1/statistics/datasets/tourist-arrivals", 200);
        assertThat(kept.get("source").get("updatedAt").asString()).isEqualTo("2026-10-19T05:00:00Z");
        assertThat(line(kept, "GB").get("values").toString()).isEqualTo("[400.0,420.0,null]");
    }

    /** A newer release replaces the table even after CYSTAT answers 429; an older copy never does. */
    @Test
    void onlyANewerReleaseReplacesATable() throws Exception {
        importAll();
        var tourist = dataset("tourist-arrivals");

        mode.set("revised");
        throttleNext.set(true);
        assertThat(refresh.refresh(tourist)).isEqualTo(RefreshResult.UPDATED);
        var revised = get("/api/v1/statistics/datasets/tourist-arrivals", 200);
        assertThat(revised.get("source").get("updatedAt").asString()).isEqualTo("2026-11-19T05:00:00Z");
        assertThat(line(revised, "total").get("values").toString()).isEqualTo("[1000.0,1100.0,1250.0]");

        mode.set("older");
        assertThat(refresh.refresh(tourist)).isEqualTo(RefreshResult.INVALID);
        var kept = get("/api/v1/statistics/datasets/tourist-arrivals", 200);
        assertThat(kept.get("source").get("updatedAt").asString()).isEqualTo("2026-11-19T05:00:00Z");
        assertThat(line(kept, "total").get("values").toString()).isEqualTo("[1000.0,1100.0,1250.0]");
    }

    private void importAll() {
        assertThat(refresh.refreshAll()).hasSize(datasets.size())
                .allSatisfy((code, result) -> assertThat(result).as(code).isEqualTo(RefreshResult.UPDATED));
    }

    private StatisticsDataset dataset(String code) {
        return datasets.stream().filter(dataset -> dataset.code().equals(code)).findFirst().orElseThrow();
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
        var response = send(path);
        assertThat(response.statusCode()).isEqualTo(status);
        return mapper.readTree(response.body());
    }

    private String cacheControl(String path) throws Exception {
        return send(path).headers().firstValue("Cache-Control").orElse(null);
    }

    private HttpResponse<String> send(String path) throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
                    HttpResponse.BodyHandlers.ofString());
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
        var answer = fixture == null ? mode.get() : faults.getOrDefault(fixture, mode.get());
        if (!"POST".equals(exchange.getRequestMethod()) || !query.contains("json-stat2") || fixture == null
                || answer.equals("unavailable")) {
            exchange.sendResponseHeaders(answer.equals("unavailable") ? 503 : 404, -1);
            exchange.close();
            return;
        }
        if (answer.equals("redirect")) {
            exchange.getResponseHeaders().add("Location", "/elsewhere");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
            return;
        }
        byte[] bytes;
        if (answer.equals("oversized")) {
            bytes = new byte[4 * 1024 * 1024 + 1];
        } else {
            String body;
            try (var input = StatisticsApiIT.class.getResourceAsStream("/cystat/" + fixture)) {
                body = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
            body = switch (answer) {
                case "invalid" -> "{\"version\":\"2.0\",\"class\":\"dataset\",\"updated\":\"2026-12-01T00:00:00Z\"}";
                // Null size and label: threw NullPointerException and stopped the whole run before the fix.
                case "malformed" -> "{\"version\":\"2.0\",\"class\":\"dataset\",\"updated\":\"2026-12-01T00:00:00Z\","
                        + "\"id\":[\"A\"],\"size\":[null],\"value\":[1],"
                        + "\"dimension\":{\"A\":{\"category\":{\"index\":{\"a\":0},\"label\":{\"a\":null}}}}}";
                case "revised" -> body.replace("2026-10-19T05:00:00Z", "2026-11-19T05:00:00Z")
                        .replace("1000, 1100, 1200,", "1000, 1100, 1250,");
                case "older" -> body.replace("1000, 1100, 1200,", "1000, 1100, 1300,");
                default -> body;
            };
            bytes = body.getBytes(StandardCharsets.UTF_8);
        }
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
