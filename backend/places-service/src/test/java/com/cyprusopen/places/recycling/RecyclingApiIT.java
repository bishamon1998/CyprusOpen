package com.cyprusopen.places.recycling;

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

/** Imports a trimmed copy of the real Green Dot file (two recycling islands, nine glass banks) and serves it. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class RecyclingApiIT {
    static final AtomicReference<String> mode = new AtomicReference<>("ok");
    static final HttpServer upstream;

    static {
        try {
            upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            upstream.createContext("/files/", exchange -> {
                var file = exchange.getRequestURI().getPath().substring("/files/".length());
                byte[] body = null;
                if (mode.get().equals("invalid")) {
                    body = "NAME_GR,Latitude\nK1,35".getBytes(StandardCharsets.UTF_8);
                } else if (!mode.get().equals("unavailable") && file.equals("db-greendot-pois_190624_toOpenData.csv")) {
                    try (var input = RecyclingApiIT.class.getResourceAsStream("/recycling/points.csv")) {
                        body = input.readAllBytes();
                    }
                }
                if (body == null) {
                    exchange.sendResponseHeaders(404, -1);
                } else {
                    exchange.getResponseHeaders().set("Last-Modified", "Mon, 23 Oct 2023 03:57:06 GMT");
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
        registry.add("recycling.files-url", () -> "http://127.0.0.1:" + upstream.getAddress().getPort() + "/files/");
        registry.add("parking.datasource.url", () -> postgres.getJdbcUrl()
                + (postgres.getJdbcUrl().contains("?") ? "&" : "?") + "currentSchema=parking");
        registry.add("parking.datasource.username", postgres::getUsername);
        registry.add("parking.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JsonMapper mapper;

    @Autowired
    private RecyclingSync sync;

    @Autowired
    private JdbcClient jdbc;

    @AfterAll
    static void stopUpstream() {
        upstream.stop(0);
    }

    @BeforeEach
    void emptyStoreAndHealthySource() {
        jdbc.sql("DELETE FROM recycling_points").update();
        mode.set("ok");
    }

    @Test
    void importsGlassBanksAndRecyclingIslands() throws Exception {
        assertThat(get().get("points").size()).isZero();
        assertThat(sync.needsInitialRun()).isTrue();

        assertThat(sync.refresh()).isEqualTo(RefreshResult.UPDATED);
        assertThat(sync.needsInitialRun()).isFalse();
        var response = get();
        assertThat(response.get("source").get("license").asString()).isEqualTo("CC BY-SA 4.0");
        // The export date in the file name, not the portal migration date in Last-Modified.
        assertThat(response.get("source").get("publishedAt").asString()).isEqualTo("2019-06-23T21:00:00Z");
        var points = response.get("points");
        assertThat(points.size()).isEqualTo(11);
        assertThat(count(points, "island")).isEqualTo(2);
        assertThat(count(points, "glass")).isEqualTo(9);

        // Codes repeat in the source, so ids come from the row.
        assertThat(points.get(2).get("code").asString()).isEqualTo("K028");
        assertThat(points.get(3).get("code").asString()).isEqualTo("K028");
        assertThat(points.get(2).get("id").asString()).isNotEqualTo(points.get(3).get("id").asString());
        // Only the "English" column describes this island; a lone dash and an empty cell are no description.
        assertThat(points.get(1).get("description").asString()).isEqualTo("Πράσινο σημείο 4 PMD, 4 Paper, 2 Glass");
        assertThat(points.get(1).get("latitude").asDouble()).isEqualTo(34.95674);
        assertThat(points.get(4).get("description").isNull()).isTrue();
        assertThat(points.get(9).get("description").isNull()).isTrue();
        assertThat(points.get(6).get("description").asString())
                .isEqualTo("Στο άδειο χωράφι δίπλα από το περίπτερο \"News & More\"");
    }

    @Test
    void sourceFailuresKeepTheLastPoints() throws Exception {
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
            var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/recycling"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            return mapper.readTree(response.body());
        }
    }

    private static long count(JsonNode points, String kind) {
        long count = 0;
        for (var point : points) if (point.get("kind").asString().equals(kind)) count++;
        return count;
    }
}
