package com.cyprusopen.common.geo;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** ISO 3166-1 alpha-2 codes for English country names, from the JDK's locale data. */
public final class Countries {
    /** Names some official sources still use that differ from the JDK's current English names. */
    private static final Map<String, String> FORMER_NAMES = Map.of("Czech Republic", "CZ", "Turkey", "TR");
    private static final Map<String, String> CODES = codes();

    private Countries() {}

    public static Optional<String> isoCode(String englishName) {
        return Optional.ofNullable(CODES.get(englishName.strip()));
    }

    private static Map<String, String> codes() {
        var codes = new HashMap<>(FORMER_NAMES);
        for (var code : Locale.getISOCountries()) codes.put(Locale.of("", code).getDisplayCountry(Locale.ENGLISH), code);
        return Map.copyOf(codes);
    }
}
