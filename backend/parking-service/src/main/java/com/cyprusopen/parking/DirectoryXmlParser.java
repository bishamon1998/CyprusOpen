package com.cyprusopen.parking;

import static com.cyprusopen.parking.DirectoryConstants.COMMON_NAMESPACE;
import static com.cyprusopen.parking.DirectoryConstants.ENGLISH_TEXT_PATTERN;
import static com.cyprusopen.parking.DirectoryConstants.LOCATION_ID_PREFIX;
import static com.cyprusopen.parking.DirectoryConstants.PARKING_NAMESPACE;
import static com.cyprusopen.parking.DirectoryConstants.UNNAMED_SOURCE_LABEL;

import com.cyprusopen.common.xml.XmlDocumentReader;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class DirectoryXmlParser {
    private final XmlDocumentReader reader;

    ParkingDirectory.Snapshot parse(byte[] bytes, ParkingDirectory.Snapshot previous, Instant retrievedAt) {
        try {
            var xml = reader.read(bytes);
            if (!"ParkingFacilitiesPublication".equals(xml.getDocumentElement().getLocalName())
                    || !PARKING_NAMESPACE.equals(xml.getDocumentElement().getNamespaceURI())) {
                throw new IllegalArgumentException("Unexpected directory publication type");
            }
            var published = OffsetDateTime.parse(text(xml.getDocumentElement(), "publicationTime")).toInstant();
            if (previous.publishedAt() != null && published.isBefore(previous.publishedAt())) {
                throw new IllegalArgumentException("Older directory publication rejected");
            }
            var nodes = xml.getElementsByTagNameNS(PARKING_NAMESPACE, "parkingFacility");
            var locations = new ArrayList<ParkingDirectory.Location>();
            for (int i = 0; i < nodes.getLength(); i++) {
                var facility = (Element) nodes.item(i);
                locations.add(new ParkingDirectory.Location(LOCATION_ID_PREFIX + text(facility, "identifier"),
                        englishName(facility), Double.parseDouble(text(facility, "latitude")),
                        Double.parseDouble(text(facility, "longitude"))));
            }
            var snapshot = new ParkingDirectory.Snapshot(previous.source(), previous.catalogUrl(), previous.resourceUrl(),
                    previous.license(), previous.licenseUrl(), published, retrievedAt, previous.transformations(), locations);
            ParkingDirectory.validate(snapshot);
            return snapshot;
        } catch (DateTimeException invalidXml) {
            throw new IllegalArgumentException("Invalid directory XML", invalidXml);
        }
    }

    private String englishName(Element facility) {
        var names = facility.getElementsByTagNameNS(PARKING_NAMESPACE, "parkingFacilityName");
        for (int i = 0; i < names.getLength(); i++) {
            var entry = (Element) names.item(i);
            if (!"en".equals(text(entry, "lang"))) continue;
            var name = text(entry, "value");
            return name.isBlank() || !name.matches(ENGLISH_TEXT_PATTERN) || name.equalsIgnoreCase(UNNAMED_SOURCE_LABEL)
                    ? null : name;
        }
        return null;
    }

    private String text(Element element, String localName) {
        return reader.requiredText(element, COMMON_NAMESPACE, localName);
    }
}
