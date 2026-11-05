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
import tools.jackson.databind.json.JsonMapper;

/** Finds the district CSV download links in the dataset's DCAT-AP metadata. */
@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class CatalogDistributions {
    private final JsonMapper mapper;

    Map<District, URI> parse(String metadata) {
        var links = new EnumMap<District, URI>(District.class);
        for (var distribution : mapper.readTree(metadata).path(DISTRIBUTIONS)) {
            var district = District.fromCatalogTitle(distribution.path(DISTRIBUTION_TITLE).asText());
            if (district.isEmpty()) continue;
            var link = URI.create(distribution.path(DOWNLOAD_URL).path(RESOURCE).asText());
            if (links.put(district.get(), link) != null) {
                throw new IllegalArgumentException("Duplicate district distribution: " + district.get().code());
            }
        }
        if (links.size() != District.values().length) {
            throw new IllegalArgumentException("Missing district distributions");
        }
        return links;
    }
}
