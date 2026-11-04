package com.cyprusopen.parking;

import static com.cyprusopen.parking.DirectoryConstants.COMMON_NAMESPACE;
import static com.cyprusopen.parking.DirectoryConstants.DISALLOW_DOCTYPE;
import static com.cyprusopen.parking.DirectoryConstants.ENGLISH_TEXT_PATTERN;
import static com.cyprusopen.parking.DirectoryConstants.LOCATION_ID_PREFIX;
import static com.cyprusopen.parking.DirectoryConstants.PARKING_NAMESPACE;
import static com.cyprusopen.parking.DirectoryConstants.UNNAMED_SOURCE_LABEL;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

@Component
class DirectoryXmlParser {
    ParkingDirectory.Snapshot parse(byte[] bytes, ParkingDirectory.Snapshot previous, Instant retrievedAt) {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature(DISALLOW_DOCTYPE, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var xml = factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
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
        } catch (IOException | SAXException | ParserConfigurationException | DateTimeException invalidXml) {
            throw new IllegalArgumentException("Invalid directory XML", invalidXml);
        }
    }

    private static String englishName(Element facility) {
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

    private static String text(Element element, String localName) {
        var nodes = element.getElementsByTagNameNS(COMMON_NAMESPACE, localName);
        if (nodes.getLength() != 1) throw new IllegalArgumentException("Missing or ambiguous directory field: " + localName);
        return nodes.item(0).getTextContent().trim();
    }
}
