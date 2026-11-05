package com.cyprusopen.pharmacies;

import java.util.Arrays;
import java.util.Optional;

/** Districts with a published duty schedule, matched by the Greek distribution title in the catalog. */
enum District {
    NICOSIA("nicosia", "Λευκωσία"),
    LARNACA("larnaca", "Λάρνακα"),
    LIMASSOL("limassol", "Λεμεσός"),
    PAPHOS("paphos", "Πάφος"),
    FAMAGUSTA("famagusta", "Αμμόχωστος");

    private final String code;
    private final String catalogTitle;

    District(String code, String catalogTitle) {
        this.code = code;
        this.catalogTitle = catalogTitle;
    }

    String code() {
        return code;
    }

    static Optional<District> fromCatalogTitle(String title) {
        return Arrays.stream(values()).filter(district -> district.catalogTitle.equals(title.strip())).findFirst();
    }
}
