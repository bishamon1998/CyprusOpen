package com.cyprusopen.common.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class XmlDocumentReaderTest {
    private static final String NAMESPACE = "urn:test";
    private final XmlDocumentReader reader = new XmlDocumentReader(1_024);

    @Test
    void readsNamespacedFields() {
        var root = reader.read(bytes("<a xmlns='urn:test'><name> Car park </name><id>1</id><id>2</id></a>"))
                .getDocumentElement();
        assertThat(reader.requiredText(root, NAMESPACE, "name")).isEqualTo("Car park");
        assertThatThrownBy(() -> reader.requiredText(root, NAMESPACE, "id")).hasMessageContaining("ambiguous");
        assertThatThrownBy(() -> reader.requiredText(root, NAMESPACE, "missing")).hasMessageContaining("Missing");
        assertThatThrownBy(() -> reader.requiredText(root, "urn:other", "name")).hasMessageContaining("Missing");
    }

    @Test
    void rejectsDoctypesSoExternalEntitiesAreNeverResolved() {
        var external = "<?xml version='1.0'?><!DOCTYPE a [<!ENTITY x SYSTEM 'file:///etc/passwd'>]>"
                + "<a xmlns='urn:test'>&x;</a>";
        assertThatThrownBy(() -> reader.read(bytes(external))).isInstanceOf(IllegalArgumentException.class);
        var expansion = "<!DOCTYPE a [<!ENTITY x 'xxxxxxxx'><!ENTITY y '&x;&x;&x;&x;'>]><a>&y;</a>";
        assertThatThrownBy(() -> reader.read(bytes(expansion))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsOversizedAndMalformedDocuments() {
        assertThatThrownBy(() -> reader.read(new byte[1_025])).hasMessageContaining("size limit");
        assertThatThrownBy(() -> reader.read(bytes("<a><b></a>"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new XmlDocumentReader(0)).isInstanceOf(IllegalArgumentException.class);
    }

    private static byte[] bytes(String xml) {
        return xml.getBytes(StandardCharsets.UTF_8);
    }
}
