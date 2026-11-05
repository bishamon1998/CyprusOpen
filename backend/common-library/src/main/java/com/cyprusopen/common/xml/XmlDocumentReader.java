package com.cyprusopen.common.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public final class XmlDocumentReader {
    private final int maximumBytes;

    public XmlDocumentReader(int maximumBytes) {
        if (maximumBytes <= 0) throw new IllegalArgumentException("XML size limit must be positive");
        this.maximumBytes = maximumBytes;
    }

    public Document read(byte[] bytes) {
        if (bytes.length > maximumBytes) throw new IllegalArgumentException("XML exceeds size limit");
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature(XmlParsingConstants.DISALLOW_DOCTYPE, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
        } catch (IOException | SAXException | ParserConfigurationException invalidXml) {
            throw new IllegalArgumentException("Invalid XML document", invalidXml);
        }
    }

    public String requiredText(Element element, String namespace, String localName) {
        var nodes = element.getElementsByTagNameNS(namespace, localName);
        if (nodes.getLength() != 1) throw new IllegalArgumentException("Missing or ambiguous XML field: " + localName);
        return nodes.item(0).getTextContent().trim();
    }
}
