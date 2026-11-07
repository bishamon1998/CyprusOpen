package com.cyprusopen.parking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cyprusopen.common.xml.XmlDocumentReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class DirectoryXmlParserTest {
    private static final Instant RETRIEVED = Instant.parse("2026-11-05T12:00:00Z");
    private static final ParkingDirectory.Snapshot EMPTY = ParkingDirectory.Snapshot.of(null, null, List.of());

    private final DirectoryXmlParser parser = new DirectoryXmlParser(new XmlDocumentReader(64 * 1024));

    @Test
    void mapsFacilitiesAndKeepsOnlyUsableEnglishNames() {
        var snapshot = parser.parse(publication("2026-11-04T00:00:00Z",
                facility("1", "en", "Faneromenis (Municipal)", "35.17", "33.36"),
                facility("2", "el", "Φανερωμένης", "35.17", "33.37"),
                facility("3", "en", "OSM Origin", "34.9", "33.6"),
                facility("4", "en", "Λεμεσός", "34.7", "33.0")), EMPTY, RETRIEVED);
        assertThat(snapshot.publishedAt()).isEqualTo(Instant.parse("2026-11-04T00:00:00Z"));
        assertThat(snapshot.retrievedAt()).isEqualTo(RETRIEVED);
        assertThat(snapshot.license()).isEqualTo(DirectoryConstants.LICENSE);
        assertThat(snapshot.items()).extracting(ParkingDirectory.Location::id)
                .containsExactly("cynap-1", "cynap-2", "cynap-3", "cynap-4");
        assertThat(snapshot.items()).extracting(ParkingDirectory.Location::name)
                .containsExactly("Faneromenis (Municipal)", null, null, null);
    }

    @Test
    void rejectsPublicationsThatWouldReplaceGoodDataWithWorse() {
        var stored = ParkingDirectory.Snapshot.of(Instant.parse("2026-11-04T00:00:00Z"), RETRIEVED, List.of());
        var one = facility("1", "en", "A", "35.1", "33.3");
        assertThatThrownBy(() -> parser.parse(publication("2026-11-03T00:00:00Z", one), stored, RETRIEVED))
                .hasMessageContaining("Older");
        assertThatThrownBy(() -> parser.parse(publication("2026-11-05T12:10:00Z", one), EMPTY, RETRIEVED))
                .hasMessageContaining("future");
        assertThatThrownBy(() -> parser.parse(publication("2026-11-04T00:00:00Z"), EMPTY, RETRIEVED))
                .hasMessageContaining("number");
        assertThatThrownBy(() -> parser.parse(publication("2026-11-04T00:00:00Z", one, one), EMPTY, RETRIEVED))
                .hasMessageContaining("Invalid parking location");
        assertThatThrownBy(() -> parser.parse(publication("2026-11-04T00:00:00Z",
                facility("1", "en", "Athens", "37.98", "23.73")), EMPTY, RETRIEVED))
                .hasMessageContaining("Invalid parking location");
        assertThatThrownBy(() -> parser.parse(publication("2026-11-04T00:00:00Z",
                facility("1", "en", "A", "north", "33.3")), EMPTY, RETRIEVED))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> parser.parse(publication("yesterday", one), EMPTY, RETRIEVED))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> parser.parse(bytes("<other xmlns='urn:x'/>"), EMPTY, RETRIEVED))
                .hasMessageContaining("publication type");
    }

    private static byte[] publication(String published, String... facilities) {
        return bytes("""
                <ParkingFacilitiesPublication xmlns="http://datex2.eu/schema/3/parking"
                    xmlns:c="http://datex2.eu/schema/3/common">
                  <c:publicationTime>%s</c:publicationTime>%s
                </ParkingFacilitiesPublication>
                """.formatted(published, String.join("", facilities)));
    }

    private static String facility(String id, String lang, String name, String latitude, String longitude) {
        return """
                <parkingFacility>
                  <c:versionedIdentifier><c:identifier>%s</c:identifier></c:versionedIdentifier>
                  <parkingFacilityName><c:lang>%s</c:lang><c:value>%s</c:value></parkingFacilityName>
                  <c:latitude>%s</c:latitude><c:longitude>%s</c:longitude>
                </parkingFacility>
                """.formatted(id, lang, name, latitude, longitude);
    }

    private static byte[] bytes(String xml) {
        return xml.getBytes(StandardCharsets.UTF_8);
    }
}
