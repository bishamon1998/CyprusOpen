package com.cyprusopen.places.parking;

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
import java.util.List;
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
class ParkingApiIT {
    static final String ETAG = "\"revision-1\"";
    static final AtomicInteger requests = new AtomicInteger();
    static final AtomicInteger redirectTargetHits = new AtomicInteger();
    static final AtomicReference<String> mode = new AtomicReference<>("ok");
    static final AtomicReference<String> ifNoneMatch = new AtomicReference<>();
    static final HttpServer upstream;

    static {
        try {
            upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            upstream.createContext("/directory", ParkingApiIT::directory);
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
            .withDatabaseName("parking").withUsername("parking_app").withPassword("integration-test-only")
            .withInitScript("parking-schema.sql");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("parking.directory.resource-url", () -> "http://127.0.0.1:" + upstream.getAddress().getPort() + "/directory");
        registry.add("parking.datasource.url", () -> postgres.getJdbcUrl() + (postgres.getJdbcUrl().contains("?") ? "&" : "?") + "currentSchema=parking");
        registry.add("parking.datasource.username", postgres::getUsername);
        registry.add("parking.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    private int port;
    @Autowired
    private JsonMapper mapper;
    @Autowired
    private DirectorySync sync;
    @Autowired
    private JdbcClient jdbc;

    @AfterAll
    static void stopUpstream() {
        upstream.stop(0);
    }

    @BeforeEach
    void emptyDirectoryAndHealthySource() {
        jdbc.sql(DirectoryTestSql.RESET).update();
        mode.set("ok");
        requests.set(0);
        redirectTargetHits.set(0);
    }

    @Test
    void servesAnEmptyDirectoryWithoutClaimingLiveAvailability() throws Exception {
        var report = parking();
        assertThat(report.get("freshness").asString()).isEqualTo("unavailable");
        assertThat(report.get("lastUpdatedAt").isNull()).isTrue();
        assertThat(report.get("directory").get("license").asString()).isEqualTo("CC BY 4.0");
        assertThat(report.get("directory").get("publishedAt").isNull()).isTrue();
        assertThat(report.get("items").size()).isZero();
    }

    /** API reads come from Postgres, never from the source; the next check is conditional and answered with 304. */
    @Test
    void importsOnceAndRechecksWithItsETag() throws Exception {
        assertThat(sync.refresh()).isEqualTo(RefreshResult.UPDATED);
        for (var i = 0; i < 3; i++) assertThat(parking().get("items").size()).isEqualTo(1);
        assertThat(requests).hasValue(1);
        var imported = parking().get("directory");
        assertThat(imported.get("publishedAt").asString()).isEqualTo("2026-11-04T00:00:00Z");

        assertThat(sync.refresh()).isEqualTo(RefreshResult.UNCHANGED);
        assertThat(ifNoneMatch.get()).isEqualTo(ETAG);
        assertThat(parking().get("directory").get("retrievedAt")).isEqualTo(imported.get("retrievedAt"));

        // Attribution comes from the current code, not from the copy kept in a document stored by an older release.
        jdbc.sql("UPDATE parking_directory SET document = jsonb_set(document, '{license}', '\"outdated\"')").update();
        assertThat(parking().get("directory").get("license").asString()).isEqualTo("CC BY 4.0");
    }

    /** Unreachable, redirecting, oversized, unsafe or older publications never replace the stored directory. */
    @Test
    void sourceFailuresKeepTheLastDirectory() throws Exception {
        assertThat(sync.refresh()).isEqualTo(RefreshResult.UPDATED);
        var stored = parking();
        for (var failure : List.of("unavailable", "redirect", "oversized", "doctype", "older")) {
            mode.set(failure);
            assertThat(sync.refresh()).as(failure)
                    .isEqualTo(failure.equals("doctype") || failure.equals("older") ? RefreshResult.INVALID : RefreshResult.UNAVAILABLE);
            assertThat(parking()).as(failure).isEqualTo(stored);
        }
        assertThat(redirectTargetHits).hasValue(0);
    }

    /** With the document gone but its ETag kept, a conditional request would get 304 forever and never re-import. */
    @Test
    void clearedDirectoryIsFetchedInFull() throws Exception {
        assertThat(sync.refresh()).isEqualTo(RefreshResult.UPDATED);
        jdbc.sql("UPDATE parking_directory SET document = NULL").update();

        assertThat(sync.refresh()).isEqualTo(RefreshResult.UPDATED);
        assertThat(ifNoneMatch.get()).isNull();
        assertThat(parking().get("items").size()).isEqualTo(1);
    }

    private JsonNode parking() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/parking")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
            return mapper.readTree(response.body());
        }
    }

    /** Behaves like the real resource: a matching If-None-Match gets 304; other modes simulate source failures. */
    private static void directory(HttpExchange exchange) throws IOException {
        requests.incrementAndGet();
        ifNoneMatch.set(exchange.getRequestHeaders().getFirst("If-None-Match"));
        switch (mode.get()) {
            case "unavailable" -> exchange.sendResponseHeaders(503, -1);
            case "redirect" -> {
                exchange.getResponseHeaders().add("Location", "/elsewhere");
                exchange.sendResponseHeaders(302, -1);
            }
            case "oversized" -> send(exchange, "\"revision-big\"", new byte[2 * 1024 * 1024 + 1]);
            case "doctype" -> send(exchange, "\"revision-2\"", fixture("directory-invalid.xml"));
            case "older" -> send(exchange, "\"revision-0\"", new String(fixture("directory.xml"), StandardCharsets.UTF_8)
                    .replace("2026-11-04T00:00:00Z", "2026-11-03T00:00:00Z").getBytes(StandardCharsets.UTF_8));
            default -> {
                if (ETAG.equals(ifNoneMatch.get())) exchange.sendResponseHeaders(304, -1);
                else send(exchange, ETAG, fixture("directory.xml"));
            }
        }
        exchange.close();
    }

    private static void send(HttpExchange exchange, String etag, byte[] body) throws IOException {
        exchange.getResponseHeaders().add("ETag", etag);
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
    }

    private static byte[] fixture(String name) throws IOException {
        try (var input = ParkingApiIT.class.getResourceAsStream("/" + name)) {
            return input.readAllBytes();
        }
    }
}
