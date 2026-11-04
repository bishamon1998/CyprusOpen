package com.cyprusopen.parking;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

@Component
class DirectoryXmlParser {
    ParkingDirectory.Snapshot parse(byte[] bytes, ParkingDirectory.Snapshot previous, Instant retrievedAt) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        var xml = factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
        if (!"ParkingFacilitiesPublication".equals(xml.getDocumentElement().getLocalName())
                || !"http://datex2.eu/schema/3/parking".equals(xml.getDocumentElement().getNamespaceURI())) {
            throw new IllegalArgumentException("Unexpected directory publication type");
        }
        var published = OffsetDateTime.parse(text(xml.getDocumentElement(), "publicationTime")).toInstant();
        if (previous.publishedAt() != null && published.isBefore(previous.publishedAt())) throw new IllegalArgumentException("Older directory publication rejected");
        var nodes = xml.getElementsByTagNameNS("http://datex2.eu/schema/3/parking", "parkingFacility");
        var locations = new ArrayList<ParkingDirectory.Location>();
        for (int i = 0; i < nodes.getLength(); i++) {
            var facility = (Element) nodes.item(i);
            String name = null;
            var names = facility.getElementsByTagNameNS("http://datex2.eu/schema/3/parking", "parkingFacilityName");
            for (int n = 0; n < names.getLength(); n++) {
                var entry = (Element) names.item(n);
                if ("en".equals(text(entry, "lang"))) name = text(entry, "value");
            }
            if (name != null && (name.isBlank() || !name.matches("[\\x20-\\x7E]+") || name.equalsIgnoreCase("OSM Origin"))) name = null;
            locations.add(new ParkingDirectory.Location("cynap-" + text(facility, "identifier"), name,
                    Double.parseDouble(text(facility, "latitude")), Double.parseDouble(text(facility, "longitude"))));
        }
        var snapshot = new ParkingDirectory.Snapshot(previous.source(), previous.catalogUrl(), previous.resourceUrl(),
                previous.license(), previous.licenseUrl(), published, retrievedAt, previous.transformations(), locations);
        ParkingDirectory.validate(snapshot);
        return snapshot;
    }

    private static String text(Element element, String localName) {
        var nodes = element.getElementsByTagNameNS("http://datex2.eu/schema/3/common", localName);
        if (nodes.getLength() != 1) throw new IllegalArgumentException("Missing or ambiguous directory field: " + localName);
        return nodes.item(0).getTextContent().trim();
    }
}
