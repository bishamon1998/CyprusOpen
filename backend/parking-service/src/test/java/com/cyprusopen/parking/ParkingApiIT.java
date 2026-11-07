package com.cyprusopen.parking;

import static org.assertj.core.api.Assertions.assertThat;

import com.cyprusopen.common.refresh.RefreshResult;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
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
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ParkingApiIT {
    static final AtomicInteger requests = new AtomicInteger();
    static final AtomicInteger mode = new AtomicInteger(200);
    static final HttpServer upstream;
    static {
        try {
            upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            upstream.createContext("/directory", exchange -> {
                requests.incrementAndGet();
                var status = mode.get();
                if (status == 304) {
                    assertThat(exchange.getRequestHeaders().getFirst("If-None-Match")).isEqualTo("\"revision-1\"");
                    exchange.sendResponseHeaders(304, -1);
                } else if (status == 503) {
                    exchange.sendResponseHeaders(503, -1);
                } else {
                    var fixture = status == 200 ? "directory.xml" : "directory-invalid.xml";
                    byte[] body;
                    try (var input = ParkingApiIT.class.getResourceAsStream("/" + fixture)) {
                        body = input.readAllBytes();
                    }
                    exchange.getResponseHeaders().add("ETag", "\"revision-1\"");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                }
                exchange.close();
            });
            upstream.start();
        } catch (IOException error) { throw new ExceptionInInitializerError(error); }
    }
    @Container
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
    @Autowired
    private JsonMapper mapper;
    @Autowired
    private ParkingDirectory directory;
    @Autowired
    private DirectorySync sync;
    @Autowired
    private JdbcClient jdbc;

    @AfterAll
    static void stopUpstream() { upstream.stop(0); }

    @Test
    void refreshesThroughHttpIntoPostgresAndPreservesGoodDataOnErrors() throws Exception {
        assertThat(directory.snapshot().items()).isEmpty();
        try {
            var before = requests.get();
            assertThat(sync.refresh()).isEqualTo(RefreshResult.UPDATED);
            assertThat(directory.snapshot().items()).hasSize(1);
            try (var client = HttpClient.newHttpClient()) {
                for (int i = 0; i < 3; i++) {
                    var api = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/parking")).build(), HttpResponse.BodyHandlers.ofString());
                    assertThat(mapper.readTree(api.body()).get("items").size()).isEqualTo(1);
                }
            }
            assertThat(requests.get()).isEqualTo(before + 1);
            assertThat(directory.snapshot().publishedAt()).isEqualTo(Instant.parse("2026-11-04T00:00:00Z"));
            // Attribution comes from the current code, not from the copy in a document stored by an older release.
            jdbc.sql("UPDATE parking_directory SET document = jsonb_set(document, '{license}', '\"outdated\"')")
                    .update();
            assertThat(directory.snapshot().license()).isEqualTo("CC BY 4.0");
            var retrieved = directory.snapshot().retrievedAt();
            mode.set(304);
            assertThat(sync.refresh()).isEqualTo(RefreshResult.UNCHANGED);
            assertThat(directory.snapshot().retrievedAt()).isEqualTo(retrieved);
            mode.set(503);
            assertThat(sync.refresh()).isEqualTo(RefreshResult.UNAVAILABLE);
            assertThat(directory.snapshot().retrievedAt()).isEqualTo(retrieved);
            mode.set(422);
            assertThat(sync.refresh()).isEqualTo(RefreshResult.INVALID);
            assertThat(directory.snapshot().items()).hasSize(1);
            assertThat(directory.snapshot().retrievedAt()).isEqualTo(retrieved);

            // Validators belong to the stored document: without one, the next request fetches the full directory.
            jdbc.sql("UPDATE parking_directory SET document = NULL").update();
            assertThat(directory.validators()).isEqualTo(new ParkingDirectory.Validators(null, null));
        } finally {
            jdbc.sql(DirectoryTestSql.RESET).update();
            mode.set(200);
        }
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
