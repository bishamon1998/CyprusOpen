package com.cyprusopen.parking;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.json.JsonMapper;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@org.testcontainers.junit.jupiter.Testcontainers
class ParkingApiIT {
    static final java.util.concurrent.atomic.AtomicInteger requests = new java.util.concurrent.atomic.AtomicInteger();
    static final java.util.concurrent.atomic.AtomicInteger mode = new java.util.concurrent.atomic.AtomicInteger(200);
    static final com.sun.net.httpserver.HttpServer upstream;
    static {
        try {
            upstream = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
            upstream.createContext("/directory", exchange -> {
                requests.incrementAndGet();
                var status = mode.get();
                if (status == 304) {
                    assertThat(exchange.getRequestHeaders().getFirst("If-None-Match")).isEqualTo("\"revision-1\"");
                    exchange.sendResponseHeaders(304, -1);
                } else if (status == 503) {
                    exchange.getResponseHeaders().add("Retry-After", "864000");
                    exchange.sendResponseHeaders(503, -1);
                } else {
                    var fixture = status == 200 ? "directory.xml" : "directory-invalid.xml";
                    var body = ParkingApiIT.class.getResourceAsStream("/" + fixture).readAllBytes();
                    exchange.getResponseHeaders().add("ETag", "\"revision-1\"");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                }
                exchange.close();
            });
            upstream.start();
        } catch (java.io.IOException error) { throw new ExceptionInInitializerError(error); }
    }
    @org.testcontainers.junit.jupiter.Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-bookworm")
            .withDatabaseName("parking").withUsername("parking_app").withPassword("integration-test-only")
            .withInitScript("parking-schema.sql");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("parking.directory.resource-url", () -> "http://127.0.0.1:" + upstream.getAddress().getPort() + "/directory");
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl() + (postgres.getJdbcUrl().contains("?") ? "&" : "?") + "currentSchema=parking");
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @LocalServerPort
    private int port;
    @org.springframework.beans.factory.annotation.Autowired
    private JsonMapper mapper;
    @org.springframework.beans.factory.annotation.Autowired
    private ParkingDirectory directory;
    @org.springframework.beans.factory.annotation.Autowired
    private DirectorySync sync;
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.simple.JdbcClient jdbc;

    @org.junit.jupiter.api.AfterAll
    static void stopUpstream() { upstream.stop(0); }

    @Test
    void refreshesThroughHttpIntoPostgresAndPreservesGoodDataOnErrors() throws Exception {
        assertThat(directory.snapshot().items()).isEmpty();
        try {
            var before = requests.get();
            assertThat(sync.refresh()).isEqualTo("updated");
            assertThat(directory.snapshot().items()).hasSize(1);
            try (var client = HttpClient.newHttpClient()) {
                for (int i = 0; i < 3; i++) {
                    var api = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/parking")).build(), HttpResponse.BodyHandlers.ofString());
                    assertThat(mapper.readTree(api.body()).get("items").size()).isEqualTo(1);
                }
            }
            assertThat(requests.get()).isEqualTo(before + 1);
            assertThat(directory.snapshot().publishedAt()).isEqualTo(java.time.Instant.parse("2026-11-04T00:00:00Z"));
            assertThat(sync.refresh()).isEqualTo("deferred");
            assertThat(requests.get()).isEqualTo(before + 1);
            var retrieved = directory.snapshot().retrievedAt();
            mode.set(304);
            allowNextRequest();
            assertThat(sync.refresh()).isEqualTo("unchanged");
            assertThat(directory.snapshot().retrievedAt()).isEqualTo(retrieved);
            mode.set(503);
            allowNextRequest();
            assertThat(sync.refresh()).isEqualTo("unavailable");
            assertThat(jdbc.sql("SELECT next_attempt_at > current_timestamp+interval '9 days' FROM parking_directory WHERE id=1").query(Boolean.class).single()).isTrue();
            assertThat(directory.snapshot().retrievedAt()).isEqualTo(retrieved);
            mode.set(422);
            allowNextRequest();
            assertThat(sync.refresh()).isEqualTo("invalid");
            assertThat(directory.snapshot().items()).hasSize(1);
            assertThat(directory.snapshot().retrievedAt()).isEqualTo(retrieved);
            mode.set(503);
            allowNextRequest();
            assertThat(sync.refresh()).isEqualTo("unavailable");
            assertThat(jdbc.sql("SELECT sync_status FROM parking_directory WHERE id=1").query(String.class).single()).isEqualTo("paused");
            var attempts = requests.get();
            allowNextRequest();
            assertThat(sync.refresh()).isEqualTo("deferred");
            assertThat(requests.get()).isEqualTo(attempts);
        } finally {
            jdbc.sql("UPDATE parking_directory SET document=NULL,last_checked_at=NULL,etag=NULL,last_modified=NULL,consecutive_failures=0,sync_status='not_imported' WHERE id=1").update();
            allowNextRequest();
            mode.set(200);
        }
    }

    private void allowNextRequest() {
        jdbc.sql("UPDATE parking_directory SET next_attempt_at=current_timestamp-interval '1 second' WHERE id=1").update();
    }

    @Test
    void servesTheReviewedDirectoryWithoutClaimingLiveAvailability() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/parking")).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
            var report = mapper.readTree(response.body());
            assertThat(report.get("freshness").asText()).isEqualTo("unavailable");
            assertThat(report.get("lastUpdatedAt").isNull()).isTrue();
            assertThat(report.get("directory").get("license").asText()).isEqualTo("CC BY 4.0");
            assertThat(report.get("directory").get("publishedAt").isNull()).isTrue();
            assertThat(report.get("items").size()).isZero();

        }
    }
}
