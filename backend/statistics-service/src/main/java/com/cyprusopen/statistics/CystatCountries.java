package com.cyprusopen.statistics;

import java.util.Map;
import java.util.Optional;

/**
 * ISO 3166-1 alpha-2 codes for country labels used in English CYSTAT tables. Labels carry notes in
 * parentheses ("Bulgaria (It is included in the E.U. since 2007 )"), which are ignored. Regions,
 * totals and countries without a current ISO code are not listed.
 */
final class CystatCountries {
    private static final Map<String, String> CODES = Map.ofEntries(
            Map.entry("Austria", "AT"), Map.entry("Belgium", "BE"), Map.entry("Bulgaria", "BG"),
            Map.entry("Czech Republic", "CZ"), Map.entry("Denmark", "DK"), Map.entry("Germany", "DE"),
            Map.entry("Estonia", "EE"), Map.entry("Greece", "GR"), Map.entry("Spain", "ES"),
            Map.entry("France", "FR"), Map.entry("Ireland", "IE"), Map.entry("Italy", "IT"),
            Map.entry("Latvia", "LV"), Map.entry("Lithuania", "LT"), Map.entry("Hungary", "HU"),
            Map.entry("Malta", "MT"), Map.entry("Netherlands", "NL"), Map.entry("Poland", "PL"),
            Map.entry("Romania", "RO"), Map.entry("Slovakia", "SK"), Map.entry("Finland", "FI"),
            Map.entry("Sweden", "SE"), Map.entry("United Kingdom", "GB"), Map.entry("Croatia", "HR"),
            Map.entry("Luxembourg", "LU"), Map.entry("Portugal", "PT"), Map.entry("Slovenia", "SI"),
            Map.entry("Georgia", "GE"), Map.entry("Switzerland", "CH"), Map.entry("Iceland", "IS"),
            Map.entry("Belarus", "BY"), Map.entry("Norway", "NO"), Map.entry("Ukraine", "UA"),
            Map.entry("Russia", "RU"), Map.entry("Serbia", "RS"), Map.entry("Turkey", "TR"),
            Map.entry("Egypt", "EG"), Map.entry("South Africa", "ZA"), Map.entry("Libya", "LY"),
            Map.entry("United States", "US"), Map.entry("Canada", "CA"), Map.entry("Kuwait", "KW"),
            Map.entry("Bahrain", "BH"), Map.entry("United Arab Emirates", "AE"), Map.entry("Saudi Arabia", "SA"),
            Map.entry("Qatar", "QA"), Map.entry("Armenia", "AM"), Map.entry("Jordan", "JO"),
            Map.entry("Israel", "IL"), Map.entry("Lebanon", "LB"), Map.entry("Japan", "JP"),
            Map.entry("Iran", "IR"), Map.entry("Iraq", "IQ"), Map.entry("Syria", "SY"),
            Map.entry("South Korea", "KR"), Map.entry("China", "CN"), Map.entry("Australia", "AU"),
            Map.entry("New Zealand", "NZ"));

    private CystatCountries() {}

    static Optional<String> code(String label) {
        var note = label.indexOf(" (");
        return Optional.ofNullable(CODES.get((note < 0 ? label : label.substring(0, note)).strip()));
    }
}
