package com.cyprusopen.places.nature;

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

/**
 * Imports trimmed copies of the real Department of Forests files (four picnic site points of which two are one site,
 * the six camping sites, three trails in six route placemarks) and serves them.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class NatureApiIT {
    static final AtomicReference<String> mode = new AtomicReference<>("ok");
    static final HttpServer upstream;

    static {
        try {
            upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            upstream.createContext("/files/", exchange -> {
                var file = exchange.getRequestURI().getPath().substring("/files/".length());
                byte[] body = null;
                if (mode.get().equals("invalid") && file.endsWith(".kml")) {
                    body = "<html><body>Maintenance</body></html>".getBytes(StandardCharsets.UTF_8);
                } else if (!mode.get().equals("unavailable") && file.matches("[A-Za-z_0-9]+\\.(kml|kmz|csv)")) {
                    try (var input = NatureApiIT.class.getResourceAsStream("/nature/" + file)) {
                        if (input != null) body = input.readAllBytes();
                    }
                }
                if (body == null) {
                    exchange.sendResponseHeaders(404, -1);
                } else {
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
        registry.add("nature.files-url", () -> "http://127.0.0.1:" + upstream.getAddress().getPort() + "/files/");
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
    private NatureSync sync;

    @Autowired
    private JdbcClient jdbc;

    @AfterAll
    static void stopUpstream() {
        upstream.stop(0);
    }

    @BeforeEach
    void emptyStoreAndHealthySource() {
        jdbc.sql("DELETE FROM nature_sites").update();
        mode.set("ok");
    }

    @Test
    void importsPicnicCampingAndTrailsWithFullGreekNames() throws Exception {
        assertThat(get().get("sites").size()).isZero();
        assertThat(sync.needsInitialRun()).isTrue();

        assertThat(sync.refresh()).isEqualTo(RefreshResult.UPDATED);
        assertThat(sync.needsInitialRun()).isFalse();
        var sites = get().get("sites");

        // Four Almyrolivado points and two Platania points are one site each.
        assertThat(names(sites, "picnic")).containsExactly("Agioi Anargyroi", "Rizoelia", "Platania", "Almyrolivado");
        assertThat(site(sites, "Rizoelia").get("greekName").isNull()).isTrue();
        assertThat(site(sites, "Agioi Anargyroi").get("greekName").asString()).isEqualTo("Άγιοι Ανάργυροι");

        // The KMZ cuts Greek names; the catalogue completes them and adds the capacity.
        assertThat(names(sites, "camping")).containsExactly("Polis", "Stavros", "Kampi tou Kaloirou", "Troodos",
                "Platania", "Governor's Beach");
        // Its fields are swapped in the source, and its cut Greek name is not in the catalogue.
        assertThat(site(sites, "Governor's Beach").get("greekName").asString()).isEqualTo("Ακτή του Κυβε…");
        assertThat(site(sites, "Polis").get("greekName").asString()).isEqualTo("Πόλις Χρυσοχούς");
        assertThat(site(sites, "Stavros").get("greekName").asString()).isEqualTo("Σταυρός Ψώκας");
        assertThat(site(sites, "Platania").get("capacity").asInt()).isEqualTo(600);
        assertThat(site(sites, "Troodos").get("greekName").asString()).isEqualTo("Τρόοδος");

        // Four route pieces of the sea caves trail are one trail; Pipis keeps its 23 lines.
        assertThat(names(sites, "trail")).containsExactly("Kakokefakos Mantra Kambiou", "Pipis",
                "Ayioi Anargyroi - Sea Caves");
        var caves = site(sites, "Ayioi Anargyroi - Sea Caves");
        assertThat(caves.get("greekName").asString()).isEqualTo("Άγιοι Ανάργυροι - Θαλασσινές Σπηλιές");
        assertThat(caves.get("route").size()).isEqualTo(4);
        assertThat(caves.get("lengthKm").asDouble()).isEqualTo(3.8);
        var cut = site(sites, "Kakokefakos Mantra Kambiou");
        assertThat(cut.get("greekName").asString()).isEqualTo("Κακοκέφαλος - Μάντρα του Καμπιού");
        assertThat(cut.get("lengthKm").asDouble()).isEqualTo(5.1);
        var pipis = site(sites, "Pipis");
        assertThat(pipis.get("route").size()).isEqualTo(23);
        var start = pipis.get("route").get(0).get(0);
        assertThat(pipis.get("latitude").asDouble()).isEqualTo(start.get(0).asDouble());
        assertThat(start.get(0).asDouble()).isBetween(34.0, 36.0);
        assertThat(start.get(1).asDouble()).isBetween(32.0, 35.0);
        // Simplified: the 440-point Kakokefalos line keeps far fewer points.
        assertThat(cut.get("route").get(0).size()).isBetween(20, 300);
    }

    @Test
    void sourceFailuresKeepTheLastSites() throws Exception {
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
            var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/nature"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            return mapper.readTree(response.body());
        }
    }

    private static java.util.List<String> names(JsonNode sites, String kind) {
        var names = new java.util.ArrayList<String>();
        for (var site : sites) if (site.get("kind").asString().equals(kind)) names.add(site.get("name").asString());
        return names;
    }

    private static JsonNode site(JsonNode sites, String name) {
        JsonNode found = null;
        for (var site : sites) {
            // Camping sites are listed after picnic sites, so a shared name resolves to the camping site.
            if (site.get("name").asString().equals(name)) found = site;
        }
        return found;
    }
}
