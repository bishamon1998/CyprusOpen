package com.cyprusopen.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
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

/**
 * The three modules in one process: each migrates only its own changelog into its own schema (they share the
 * changelog path), and every API answers. Each module's refresh and API behavior is tested in the module itself.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class BackendApplicationIT {
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-bookworm")
            .withDatabaseName("backend").withUsername("backend_app").withPassword("integration-test-only")
            .withInitScript("backend-schemas.sql");

    @DynamicPropertySource
    static void databases(DynamicPropertyRegistry registry) {
        for (var module : List.of("statistics", "parking", "pharmacies")) {
            registry.add(module + ".datasource.url", () -> postgres.getJdbcUrl()
                    + (postgres.getJdbcUrl().contains("?") ? "&" : "?") + "currentSchema=" + module);
            registry.add(module + ".datasource.username", postgres::getUsername);
            registry.add(module + ".datasource.password", postgres::getPassword);
        }
    }

    @LocalServerPort
    private int port;

    @Test
    void runsEveryModuleWithItsOwnMigrations() throws Exception {
        var jdbc = JdbcClient.create(new org.springframework.jdbc.datasource.DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
        assertThat(changesets(jdbc, "statistics")).containsExactly("1-observations", "2-quarter-periods");
        assertThat(changesets(jdbc, "parking")).contains("1-parking-directory", "4-air-quality", "5-beaches", "6-nature")
                .hasSize(6);
        assertThat(changesets(jdbc, "pharmacies")).contains("1-duty-schedule", "2-pharmacy-location", "3-schedule-source-date")
                .hasSize(8);

        try (var client = HttpClient.newHttpClient()) {
            for (var path : List.of("/api/v1/statistics/datasets", "/api/v1/parking", "/api/v1/air-quality",
                    "/api/v1/beaches", "/api/v1/nature", "/api/v1/pharmacies", "/actuator/health/readiness")) {
                var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
                        HttpResponse.BodyHandlers.ofString());
                assertThat(response.statusCode()).as(path).isEqualTo(200);
            }
        }
    }

    private static List<String> changesets(JdbcClient jdbc, String schema) {
        return jdbc.sql("SELECT id FROM " + schema + ".databasechangelog ORDER BY orderexecuted").query(String.class)
                .list();
    }
}
