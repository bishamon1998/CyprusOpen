package com.cyprusopen.pharmacies;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
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
class PharmacyScheduleIT {
    /** 22:00 in Cyprus on 1 November 2026, when winter overnight on-call service has started. */
    static final Instant NOW = Instant.parse("2026-11-01T20:00:00Z");
    static final String HEADERS = "Date,Day,Reg. No.,Surmame,Name,Address,Additional Address Info,"
            + "Muniuciplity / Community,Pharmacy Tel. No.,House Tel. No.\n";
    /** Catalog distribution titles as published in Greek. */
    static final String[][] DISTRICTS = {
            {"nicosia", "Λευκωσία"},
            {"larnaca", "Λάρνακα"},
            {"limassol", "Λεμεσός"},
            {"paphos", "Πάφος"},
            {"famagusta", "Αμμόχωστος"}};
    static final AtomicReference<String> mode = new AtomicReference<>("initial");
    static final HttpServer upstream;

    static {
        try {
            upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            upstream.createContext("/metadata", PharmacyScheduleIT::metadata);
            upstream.createContext("/csv/", PharmacyScheduleIT::schedule);
            upstream.start();
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-bookworm")
            .withDatabaseName("pharmacies").withUsername("pharmacies_app").withPassword("integration-test-only")
            .withInitScript("pharmacies-schema.sql");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("pharmacies.catalog-metadata-url", () -> origin() + "/metadata");
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl()
                + (postgres.getJdbcUrl().contains("?") ? "&" : "?") + "currentSchema=pharmacies");
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @TestConfiguration
    static class FixedClock {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JsonMapper mapper;

    @Autowired
    private PharmacyScheduleRefresh refresh;

    @Autowired
    private JdbcClient jdbc;

    @AfterAll
    static void stopUpstream() {
        upstream.stop(0);
    }

    @Test
    void refreshesAllDistrictsIntoPostgresAndKeepsTheRosterOnSourceErrors() throws Exception {
        var empty = schedule("");
        assertThat(empty.get("items").size()).isZero();
        assertThat(empty.get("checkedAt").isNull()).isTrue();

        assertThat(refresh.refresh()).isEqualTo(RefreshResult.UPDATED);
        jdbc.sql("""
                INSERT INTO pharmacy_location (registration_number, latitude, longitude, precision, source)
                VALUES ('10', 35.17, 33.36, 'street', 'manual review')
                """).update();
        var current = schedule("");
        assertThat(current.get("date").asText()).isEqualTo("2026-11-01");
        assertThat(current.get("onCall").asBoolean()).isTrue();
        assertThat(current.get("checkedAt").asText()).isEqualTo("2026-11-01T20:00:00Z");
        assertThat(current.get("items").size()).isEqualTo(5);
        assertThat(current.toString()).doesNotContain("99123456");
        var nicosia = item(current, "nicosia:10");
        assertThat(nicosia.get("address").asText()).isEqualTo("First Street 1, Shop 2");
        assertThat(nicosia.get("location").get("latitude").asDouble()).isEqualTo(35.17);
        assertThat(nicosia.get("location").get("precision").asText()).isEqualTo("street");
        assertThat(nicosia.get("location").get("source").asText()).isEqualTo("manual review");
        assertThat(item(current, "paphos:40").get("location").isNull()).isTrue();

        var selected = schedule("?date=2026-11-02");
        assertThat(selected.get("onCall").asBoolean()).isFalse();
        assertThat(item(selected, "nicosia:11").get("name").asText()).isEqualTo("Sam Example");

        mode.set("invalid");
        assertThat(refresh.refresh()).isEqualTo(RefreshResult.INVALID);
        mode.set("unavailable");
        assertThat(refresh.refresh()).isEqualTo(RefreshResult.UNAVAILABLE);
        mode.set("foreign");
        assertThat(refresh.refresh()).isEqualTo(RefreshResult.UNAVAILABLE);
        assertThat(schedule("").get("items").size()).isEqualTo(5);

        mode.set("replacement");
        assertThat(refresh.refresh()).isEqualTo(RefreshResult.UPDATED);
        assertThat(item(schedule(""), "nicosia:10")).isNotNull();
        var replaced = schedule("?date=2026-11-02");
        assertThat(item(replaced, "nicosia:12").get("name").asText()).isEqualTo("Jo Example");
        assertThat(item(replaced, "nicosia:11")).isNull();

        assertThat(schedule("?date=2026-11-05").get("items").size()).isZero();
    }

    private JsonNode schedule(String query) throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(HttpRequest.newBuilder(
                    URI.create("http://localhost:" + port + "/api/v1/pharmacies" + query)).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
            return mapper.readTree(response.body());
        }
    }

    private static JsonNode item(JsonNode schedule, String id) {
        for (var item : schedule.get("items")) {
            if (item.get("id").asText().equals(id)) return item;
        }
        return null;
    }

    private static String origin() {
        return "http://127.0.0.1:" + upstream.getAddress().getPort();
    }

    private static void metadata(HttpExchange exchange) throws IOException {
        if (mode.get().equals("unavailable")) {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
            return;
        }
        var host = mode.get().equals("foreign") ? "http://localhost:" + upstream.getAddress().getPort() : origin();
        var distributions = new StringBuilder();
        for (var district : DISTRICTS) {
            if (!distributions.isEmpty()) distributions.append(',');
            distributions.append("{\"dct:title\":\"").append(district[1])
                    .append("\",\"dcat:downloadURL\":{\"@rdf:resource\":\"").append(host).append("/csv/")
                    .append(district[0]).append("\"}}");
        }
        respond(exchange, "{\"dcat:Distribution\":[" + distributions + "]}");
    }

    private static void schedule(HttpExchange exchange) throws IOException {
        var district = exchange.getRequestURI().getPath().substring("/csv/".length());
        String body;
        if (!district.equals("nicosia")) {
            var id = switch (district) {
                case "larnaca" -> 20;
                case "limassol" -> 30;
                case "paphos" -> 40;
                default -> 50;
            };
            body = HEADERS + "01/11/2026,Sunday," + id + ",Example,Alex,Second Street " + id + ",,Town,26123456,\n";
        } else {
            body = switch (mode.get()) {
                case "invalid" -> "Unexpected,Columns\n1,2\n";
                case "replacement" -> HEADERS + "02/11/26,Monday,12,Example,Jo,Third Street 3,,Nicosia,22123458,\n";
                default -> "﻿" + HEADERS
                        + "01/11/26,Sunday,10,Example,Alex,\"First Street 1, Shop 2\",,Nicosia,22123456,99123456\n"
                        + "02/11/26,Monday,11,Example,Sam,Fourth Street 4,,Nicosia,22123457,\n";
            };
        }
        respond(exchange, body);
    }

    private static void respond(HttpExchange exchange, String body) throws IOException {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
