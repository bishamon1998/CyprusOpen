package com.cyprusopen.places.parking;

import static com.cyprusopen.places.parking.DirectoryConstants.COMMON_NAMESPACE;
import static com.cyprusopen.places.parking.DirectoryConstants.ENGLISH_LANGUAGE;
import static com.cyprusopen.places.parking.DirectoryConstants.ENGLISH_TEXT_PATTERN;
import static com.cyprusopen.places.parking.DirectoryConstants.LOCATION_ID_PREFIX;
import static com.cyprusopen.places.parking.DirectoryConstants.MAX_CLOCK_SKEW;
import static com.cyprusopen.places.parking.DirectoryConstants.MAX_LOCATIONS;
import static com.cyprusopen.places.parking.DirectoryConstants.PARKING_NAMESPACE;
import static com.cyprusopen.places.parking.DirectoryConstants.PUBLICATION_ELEMENT;
import static com.cyprusopen.places.parking.DirectoryConstants.UNNAMED_SOURCE_LABEL;

import com.cyprusopen.common.geo.CyprusBounds;
import com.cyprusopen.common.xml.XmlDocumentReader;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

/** Maps a DATEX II parking facilities publication to a validated directory snapshot. */
@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class DirectoryXmlParser {
    private final XmlDocumentReader reader;

    ParkingDirectory.Snapshot parse(byte[] bytes, ParkingDirectory.Snapshot previous, Instant retrievedAt) {
        try {
            var root = reader.read(bytes).getDocumentElement();
            if (!PUBLICATION_ELEMENT.equals(root.getLocalName()) || !PARKING_NAMESPACE.equals(root.getNamespaceURI())) {
                throw new IllegalArgumentException("Unexpected directory publication type");
            }
            var published = OffsetDateTime.parse(text(root, "publicationTime")).toInstant();
            if (published.isAfter(retrievedAt.plus(MAX_CLOCK_SKEW))) {
                throw new IllegalArgumentException("Directory publication time is in the future");
            }
            if (previous.publishedAt() != null && published.isBefore(previous.publishedAt())) {
                throw new IllegalArgumentException("Older directory publication rejected");
            }
            var nodes = root.getElementsByTagNameNS(PARKING_NAMESPACE, "parkingFacility");
            if (nodes.getLength() == 0 || nodes.getLength() > MAX_LOCATIONS) {
                throw new IllegalArgumentException("Unexpected number of parking locations");
            }
            var identifiers = new HashSet<String>();
            var locations = new ArrayList<ParkingDirectory.Location>();
            for (int i = 0; i < nodes.getLength(); i++) {
                var location = location((Element) nodes.item(i));
                if (!identifiers.add(location.id())
                        || !CyprusBounds.contains(location.latitude(), location.longitude())) {
                    throw new IllegalArgumentException("Invalid parking location");
                }
                locations.add(location);
            }
            return ParkingDirectory.Snapshot.of(published, retrievedAt, locations);
        } catch (DateTimeException invalidXml) {
            throw new IllegalArgumentException("Invalid directory XML", invalidXml);
        }
    }

    private ParkingDirectory.Location location(Element facility) {
        return new ParkingDirectory.Location(LOCATION_ID_PREFIX + text(facility, "identifier"), englishName(facility),
                Double.parseDouble(text(facility, "latitude")), Double.parseDouble(text(facility, "longitude")));
    }

    private String englishName(Element facility) {
        var names = facility.getElementsByTagNameNS(PARKING_NAMESPACE, "parkingFacilityName");
        for (int i = 0; i < names.getLength(); i++) {
            var entry = (Element) names.item(i);
            if (!ENGLISH_LANGUAGE.equals(text(entry, "lang"))) continue;
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
