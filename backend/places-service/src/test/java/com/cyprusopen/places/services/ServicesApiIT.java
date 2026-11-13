package com.cyprusopen.places.services;

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
import java.util.ArrayList;
import java.util.List;
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

/** Imports copies of the real hospital (8 rows) and Citizen Service Centre (9 rows) files and serves them. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ServicesApiIT {
    static final AtomicReference<String> mode = new AtomicReference<>("ok");
    static final HttpServer upstream;

    static {
        try {
            upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            upstream.createContext("/files/", exchange -> {
                // The decoded data.gov.cy file names: "Κατάλογος Νοσοκομείων Κύπρου_1.csv" and the CSC file.
                var file = exchange.getRequestURI().getPath().substring("/files/".length());
                var fixture = file.equals("Κατάλογος Νοσοκομείων Κύπρου_1.csv") ? "hospitals.csv"
                        : file.equals("CSC Info and Location Data_5.csv") ? "centres.csv" : null;
                byte[] body = null;
                if (mode.get().equals("invalid") && "centres.csv".equals(fixture)) {
                    body = "<html><body>Maintenance</body></html>".getBytes(StandardCharsets.UTF_8);
                } else if (!mode.get().equals("unavailable") && fixture != null) {
                    try (var input = ServicesApiIT.class.getResourceAsStream("/services/" + fixture)) {
                        if (input != null) body = input.readAllBytes();
                    }
                }
                if (body == null) {
                    exchange.sendResponseHeaders(404, -1);
                } else {
                    exchange.getResponseHeaders().set("Last-Modified", "centres.csv".equals(fixture)
                            ? "Tue, 20 Jan 2026 09:07:22 GMT" : "Mon, 23 Oct 2023 03:56:55 GMT");
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
        registry.add("services.files-url", () -> "http://127.0.0.1:" + upstream.getAddress().getPort() + "/files/");
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
    private ServicesSync sync;

    @Autowired
    private JdbcClient jdbc;

    @AfterAll
    static void stopUpstream() {
        upstream.stop(0);
    }

    @BeforeEach
    void emptyStoreAndHealthySource() {
        jdbc.sql("DELETE FROM service_places").update();
        mode.set("ok");
    }

    @Test
    void importsHospitalsAndCentresWithTheirDates() throws Exception {
        var empty = get();
        assertThat(empty.get("places").size()).isZero();
        assertThat(empty.get("sources").get("hospitals").get("name").asString()).isEqualTo("Ministry of Health");
        assertThat(sync.needsInitialRun()).isTrue();

        assertThat(sync.refresh()).isEqualTo(RefreshResult.UPDATED);
        assertThat(sync.needsInitialRun()).isFalse();
        var response = get();
        var sources = response.get("sources");
        assertThat(sources.get("hospitals").get("publishedAt").asString()).isEqualTo("2023-10-23T03:56:55Z");
        assertThat(sources.get("centres").get("publishedAt").asString()).isEqualTo("2026-01-20T09:07:22Z");
        assertThat(sources.get("centres").get("license").asString()).isEqualTo("CC BY 4.0");
        assertThat(sources.get("hospitals").get("retrievedAt").isNull()).isFalse();

        var places = response.get("places");
        assertThat(names(places, "hospital")).hasSize(8).contains("Γενικό Νοσοκομείο Λευκωσίας");
        assertThat(names(places, "centre")).hasSize(9).contains("ΚΕΠ Λεμεσού");

        // A line break inside the quoted address becomes a space.
        var polis = place(places, "Νοσοκομείο Πόλης Χρυσοχούς");
        assertThat(polis.get("address").asString()).isEqualTo("Βεργίνας 25, 8820 Πόλης Χρυσοχούς");
        assertThat(polis.get("phone").asString()).isEqualTo("26821800");
        assertThat(polis.get("hours").isNull()).isTrue();
        assertThat(polis.get("latitude").asDouble()).isEqualTo(35.0394);

        var limassol = place(places, "ΚΕΠ Λεμεσού");
        assertThat(limassol.get("hours").get("days").toString()).isEqualTo("[1,2,3,4,5]");
        assertThat(limassol.get("hours").get("opens").asString()).isEqualTo("08:00");
        assertThat(limassol.get("hours").get("closes").asString()).isEqualTo("17:00");
        assertThat(limassol.get("hoursText").asString()).isEqualTo("Δευτερα - Παρασκευή, 8:00 - 17:00");
        assertThat(limassol.get("appointmentUrl").asString()).isEqualTo("https://kep-kepo.gov.cy/appointments");

        // The Kato Pyrgos unit opens one morning a week.
        var pyrgos = place(places, "Μονάδα ΚΕΠ Κάτω Πύργου Τηλληρίας");
        assertThat(pyrgos.get("hours").get("days").toString()).isEqualTo("[4]");
        assertThat(pyrgos.get("hours").get("closes").asString()).isEqualTo("13:45");
    }

    @Test
    void sourceFailuresKeepTheLastPlaces() throws Exception {
        assertThat(sync.refresh()).isEqualTo(RefreshResult.UPDATED);
        var stored = get();
        mode.set("invalid");
        assertThat(sync.refresh()).isEqualTo(RefreshResult.INVALID);
        mode.set("unavailable");
        assertThat(sync.refresh()).isEqualTo(RefreshResult.UNAVAILABLE);
        assertThat(get()).isEqualTo(stored);
    }

    @Test
    void readsOnlyOneRangeOfDaysWithOneTimeRange() {
        assertThat(ServicesParser.hours("Τρίτη - Πέμπτη, 7.30 - 15.00").days()).containsExactly(2, 3, 4);
        assertThat(ServicesParser.hours("Τρίτη - Πέμπτη, 7.30 - 15.00").opens()).isEqualTo("07:30");
        assertThat(ServicesParser.hours("Δευτέρα - Παρασκευή, 8:00 - 13:00 και 15:00 - 18:00")).isNull();
        assertThat(ServicesParser.hours("Παρασκευή - Δευτέρα, 8:00 - 13:00")).isNull();
        assertThat(ServicesParser.hours("Κατόπιν ραντεβού")).isNull();
    }

    private JsonNode get() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/services"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            return mapper.readTree(response.body());
        }
    }

    private static List<String> names(JsonNode places, String kind) {
        var names = new ArrayList<String>();
        for (var place : places) if (place.get("kind").asString().equals(kind)) names.add(place.get("name").asString());
        return names;
    }

    private static JsonNode place(JsonNode places, String name) {
        for (var place : places) if (place.get("name").asString().equals(name)) return place;
        throw new AssertionError("No place " + name);
    }
}
