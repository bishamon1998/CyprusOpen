package com.cyprusopen.pharmacies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.StringReader;
import java.net.URI;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** Catalog metadata and district CSVs are untrusted: every unexpected shape is an {@link IllegalArgumentException}. */
class PharmacySourceParsingTest {
    private static final String HEADERS = "Date,Day,Reg. No.,Surmame,Name,Address,Additional Address Info,"
            + "Muniuciplity / Community,Pharmacy Tel. No.,House Tel. No.\n";

    private final PharmacyCsvReader reader = new PharmacyCsvReader();
    private final CatalogDistributions distributions = new CatalogDistributions(JsonMapper.builder().build());

    @Test
    void readsShortAndLongDatesSkipsBlankRowsAndDropsHouseNumbers() throws Exception {
        var entries = reader.read(new StringReader("﻿" + HEADERS
                + "01/11/26,Sunday,10,Example,Alex,\"First Street 1, Shop 2\",,Nicosia,22123456,99123456\n"
                + ",,,,,,,,,\n"
                + "2/11/2026,Monday,11,Example,Sam,Fourth Street 4,,Nicosia,22123457,\n"), District.NICOSIA);
        assertThat(entries).extracting(DutyEntry::date)
                .containsExactly(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2));
        assertThat(entries.getFirst()).isEqualTo(new DutyEntry(LocalDate.of(2026, 11, 1), "nicosia", "10",
                "Alex Example", "First Street 1, Shop 2", "Nicosia", "22123456"));
    }

    @Test
    void rejectsUnexpectedCsv() {
        var row = "01/11/26,Sunday,10,Example,Alex,Street 1,,Nicosia,22123456,\n";
        for (var invalid : new String[] {
                "Unexpected,Columns\n1,2\n",
                HEADERS + row + row,
                HEADERS + row.replace(",10,", ",A10,"),
                HEADERS + row.replace("Street 1", ""),
                HEADERS + row.replace("01/11/26", "31/02/26"),
                HEADERS + row.replace("01/11/26", "tomorrow"),
                HEADERS + "01/11/26,Sunday,10\n",
                HEADERS + "01/11/26,Sunday,10,Example,Alex,\"Street 1,,Nicosia,22123456,\n"}) {
            assertThatThrownBy(() -> reader.read(new StringReader(invalid), District.NICOSIA)).as(invalid)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void findsOneDownloadLinkPerDistrict() {
        var links = distributions.parse(metadata("https://www.data.gov.cy/"));
        assertThat(links).hasSize(5)
                .containsEntry(District.PAPHOS, URI.create("https://www.data.gov.cy/paphos.csv"));
    }

    @Test
    void rejectsUnexpectedCatalogMetadata() {
        var valid = metadata("https://www.data.gov.cy/");
        for (var invalid : new String[] {
                "{",
                "[]",
                valid.replace("Πάφος", "Κερύνεια"),
                valid.replace("\"Λάρνακα\"", "\"Λευκωσία\""),
                valid.replace("\"@rdf:resource\":\"https://www.data.gov.cy/paphos.csv\"", "\"other\":1"),
                valid.replace("https://www.data.gov.cy/paphos.csv", "https://www.data.gov.cy/a b.csv")}) {
            assertThatThrownBy(() -> distributions.parse(invalid)).as(invalid)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    private static String metadata(String origin) {
        var distributions = new StringBuilder();
        for (var district : new String[][] {{"nicosia", "Λευκωσία"}, {"larnaca", "Λάρνακα"},
                {"limassol", "Λεμεσός"}, {"paphos", "Πάφος"}, {"famagusta", "Αμμόχωστος"}}) {
            if (!distributions.isEmpty()) distributions.append(',');
            distributions.append("{\"dct:title\":\"").append(district[1])
                    .append("\",\"dcat:downloadURL\":{\"@rdf:resource\":\"").append(origin).append(district[0])
                    .append(".csv\"}}");
        }
        return "{\"dcat:Distribution\":[" + distributions + ",{\"dct:title\":\"Other\"}]}";
    }
}
