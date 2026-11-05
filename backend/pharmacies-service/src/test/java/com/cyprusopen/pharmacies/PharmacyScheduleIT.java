package com.cyprusopen.pharmacies;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PharmacyScheduleIT {
    @TempDir
    static Path schedules;

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry properties) throws Exception {
        var headers = "Date,Day,Reg. No.,Surmame,Name,Address,Additional Address Info,Muniuciplity / Community,Pharmacy Tel. No.,House Tel. No.\n";
        Files.writeString(schedules.resolve("nicosia.csv"), "\uFEFF" + headers
                + "01/11/26,Sunday,10,Example,Alex,\"First Street 1, Shop 2\",,Nicosia,22123456,99123456\n");
        Files.writeString(schedules.resolve("paphos.csv"), headers
                + "01/11/2026,Sunday,20,Example,Sam,Second Street 2,,Paphos,26123456,99123456\n"
                + "02/11/2026,Monday,30,Example,Jo,Third Street 3,,Paphos,26123457,99123456\n");
        properties.add("pharmacies.schedules.directory", schedules::toString);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JsonMapper mapper;

    @Test
    void servesOnlyTheRequestedDutyDateWithoutPersonalPhoneNumbers() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var endpoint = "http://localhost:" + port + "/api/v1/pharmacies?date=";
            var response = client.send(HttpRequest.newBuilder(URI.create(endpoint + "2026-11-01")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).doesNotContain("99123456", "Third Street");
            var items = mapper.readTree(response.body()).get("items");
            assertThat(items.size()).isEqualTo(2);
            assertThat(items.get(0).get("address").asText()).isEqualTo("First Street 1, Shop 2");
            assertThat(items.get(1).get("district").asText()).isEqualTo("paphos");

            var absent = client.send(HttpRequest.newBuilder(URI.create(endpoint + "2026-11-03")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(absent.statusCode()).isEqualTo(200);
            assertThat(mapper.readTree(absent.body()).get("items").size()).isZero();
        }
    }
}
