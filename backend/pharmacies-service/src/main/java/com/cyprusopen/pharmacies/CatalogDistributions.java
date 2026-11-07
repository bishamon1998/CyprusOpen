package com.cyprusopen.pharmacies;

import static com.cyprusopen.pharmacies.PharmacyConstants.DISTRIBUTIONS;
import static com.cyprusopen.pharmacies.PharmacyConstants.DISTRIBUTION_TITLE;
import static com.cyprusopen.pharmacies.PharmacyConstants.DOWNLOAD_URL;
import static com.cyprusopen.pharmacies.PharmacyConstants.RESOURCE;

import java.net.URI;
import java.util.EnumMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Finds the district CSV download links in the dataset's DCAT-AP metadata. */
@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class CatalogDistributions {
    private final JsonMapper mapper;

    /** Throws {@link IllegalArgumentException} for metadata that is not JSON or lacks a district link. */
    Map<District, URI> parse(String metadata) {
        var links = new EnumMap<District, URI>(District.class);
        for (var distribution : tree(metadata).path(DISTRIBUTIONS)) {
            var district = District.fromCatalogTitle(distribution.path(DISTRIBUTION_TITLE).asString(""));
            if (district.isEmpty()) continue;
            var link = distribution.path(DOWNLOAD_URL).path(RESOURCE).asString("");
            if (link.isBlank()) throw new IllegalArgumentException("Missing download link: " + district.get().code());
            if (links.put(district.get(), URI.create(link)) != null) {
                throw new IllegalArgumentException("Duplicate district distribution: " + district.get().code());
            }
        }
        if (links.size() != District.values().length) {
            throw new IllegalArgumentException("Missing district distributions");
        }
        return links;
    }

    private JsonNode tree(String metadata) {
        try {
            return mapper.readTree(metadata);
        } catch (JacksonException invalid) {
            throw new IllegalArgumentException("Catalog metadata is not JSON", invalid);
        }
    }
}
