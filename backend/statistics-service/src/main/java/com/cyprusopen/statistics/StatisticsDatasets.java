package com.cyprusopen.statistics;

import static com.cyprusopen.statistics.SeriesMapping.labels;
import static com.cyprusopen.statistics.StatisticsConstants.TOTAL_SERIES;
import static java.util.Map.entry;

import com.cyprusopen.common.geo.Countries;
import java.time.Month;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Every CYSTAT table the statistics API serves: its code, table path, data.gov.cy page and how its
 * categories map to series keys. Fetching, storage and refresh are shared; a new chart adds one bean here.
 */
@Configuration
class StatisticsDatasets {
    private static final String YEAR = "YEAR";
    private static final String MONTH = "MONTH";

    // Migration

    /** Long-term immigrants by citizenship, birth and previous residence; CYSTAT publishes only Cyprus / EU / non-EU. */
    @Bean
    @Order(10)
    StatisticsDataset immigrantGroups() {
        return CystatDataset.of("immigrant-groups", "Population/Migration/1840030E.px", 2174, table -> SeriesMapping.map(
                table, "CITIZENSHIP, COUNTRY OF BIRTH, COUNTRY OF PREVIOUS RESIDENCE", labels(Map.ofEntries(
                        entry("Total Immigrants", TOTAL_SERIES),
                        entry("Citizenship - Cypriots", "citizenship-cyprus"),
                        entry("Citizenship - EU", "citizenship-eu"),
                        entry("Citizenship - Non EU", "citizenship-non-eu"),
                        entry("Country of birth - Cyprus", "birth-cyprus"),
                        entry("Country of birth - EU", "birth-eu"),
                        entry("Country of birth - Non EU", "birth-non-eu"),
                        entry("Country of previous residence - EU", "previous-residence-eu"),
                        entry("Country of previous residence - Non EU", "previous-residence-non-eu"))),
                YEAR, Map.of("SEX", "Total", "AGE", "Total")));
    }

    @Bean
    @Order(20)
    StatisticsDataset migrationMovements() {
        return CystatDataset.of("migration-movements", "Population/Migration/1840010E.px", 2172, table -> SeriesMapping.map(
                table, "MIGRATION MOVEMENTS", labels(Map.of(
                        "Total of long term immigrants", "immigrants",
                        "Emigrants", "emigrants",
                        "Net migration", "net-migration")),
                YEAR, Map.of()));
    }

    // Tourism

    /** Series are ISO country codes plus the total; regions and countries without a current code are skipped. */
    @Bean
    @Order(30)
    StatisticsDataset touristArrivals() {
        return CystatDataset.of("tourist-arrivals", "Tourism/Tourists/Monthly/2021010E.px", 2285, table -> SeriesMapping.map(
                table, "COUNTRY OF USUAL RESIDENCE",
                label -> "TOTAL".equals(label) ? Optional.of(TOTAL_SERIES) : Countries.isoCode(withoutNotes(label)),
                MONTH, Map.of()));
    }

    // Prices

    /** Annual inflation rate (%), since 1960. */
    @Bean
    @Order(40)
    StatisticsDataset inflation() {
        return CystatDataset.of("inflation", "Price Indices/Consumer Price Index/0410050E.px", 2201,
                table -> SeriesMapping.single(table, YEAR, Map.of(), "inflation"));
    }

    /** Annual price change (%) by main consumer category. */
    @Bean
    @Order(50)
    StatisticsDataset priceChanges() {
        return CystatDataset.of("price-changes", "Price Indices/Consumer Price Index/0410071E.px", 2314,
                table -> SeriesMapping.map(table, "COICOP18", labels(Map.ofEntries(
                        entry("General Consumer Price Index", TOTAL_SERIES),
                        entry("Food and non-alcoholic beverages", "food"),
                        entry("Alcoholic beverages and tobacco", "alcohol-tobacco"),
                        entry("Clothing and footwear", "clothing"),
                        entry("Housing, water, electricity, gas and other fuels", "housing"),
                        entry("Furnishings, household equipment and routine household maintenance", "furnishings"),
                        entry("Health", "health"),
                        entry("Transport", "transport"),
                        entry("Information and communication", "communication"),
                        entry("Recreation, sport and culture", "recreation"),
                        entry("Education services", "education"),
                        entry("Restaurants and accommodation services", "restaurants"),
                        entry("Insurance and financial services", "insurance"),
                        entry("Personal care, social protection and miscellaneous goods and services", "personal-care"))),
                        YEAR, Map.of("INDICATOR", "% change - previous year")));
    }

    // Population

    /** Mid-year population, in thousands. */
    @Bean
    @Order(60)
    StatisticsDataset population() {
        return CystatDataset.of("population", "Population/Population/1820010E.px", 2176, table -> SeriesMapping.map(
                table, "POPULATION/PERCENTAGE CHANGE", labels(Map.of(
                        "Total (000's)", TOTAL_SERIES, "Males (000's)", "males", "Females (000's)", "females")),
                YEAR, Map.of("PERIOD", "Mid year")));
    }

    @Bean
    @Order(70)
    StatisticsDataset populationChange() {
        return CystatDataset.of("population-change", "Population/Population/1820015E.px", 2177, table -> SeriesMapping.map(
                table, "INCREASE/MIGRATION/CHANGE", labels(Map.of(
                        "Natural Increase", "natural-increase", "Net Migration", "net-migration",
                        "Population Change", "change")),
                YEAR, Map.of("NUMBER/RATES", "Number")));
    }

    /** Life expectancy at birth, in years. */
    @Bean
    @Order(80)
    StatisticsDataset lifeExpectancy() {
        return CystatDataset.of("life-expectancy", "Population/Deaths/1830226E.px", 2142, table -> SeriesMapping.map(
                table, "LIFE EXPECTANCY AT BIRTH", labels(Map.of("Males", "males", "Females", "females")),
                YEAR, Map.of()));
    }

    // Marriages and civil unions

    /** All marriages registered in Cyprus, including non-residents, plus residents only. */
    @Bean
    @Order(90)
    StatisticsDataset marriages() {
        return CystatDataset.of("marriages", "Population/Marriages/1850010E.px", 2157, table -> SeriesMapping.map(
                table, "TYPE OF MARRIAGE, MARRIAGE RATES", labels(Map.of(
                        "Total Marriages", TOTAL_SERIES, "Ecclesiastical Marriages", "religious",
                        "Civil Marriages", "civil", "Total Marriages - Residents", "residents")),
                YEAR, Map.of()));
    }

    @Bean
    @Order(100)
    StatisticsDataset civilUnions() {
        return CystatDataset.of("civil-unions", "Population/Marriages/Civil Unions/1850105E.px", 2352,
                table -> SeriesMapping.map(table, "CIVIL UNIONS", labels(Map.of(
                        "Total Civil Unions", TOTAL_SERIES, "Opposite sex", "opposite-sex",
                        "Same sex (Males)", "same-sex-men", "Same sex (Females)", "same-sex-women")),
                        YEAR, Map.of()));
    }

    // Calendar

    /** Live births by calendar date of birth. */
    @Bean
    @Order(103)
    StatisticsDataset birthDates() {
        return calendar("birth-dates", "Population/Births and Deaths/1830018E.px", 2079);
    }

    /** Marriages of residents by calendar date of the wedding. */
    @Bean
    @Order(106)
    StatisticsDataset weddingDates() {
        return calendar("wedding-dates", "Population/Marriages/1850066E.px", 2171);
    }

    /** Counts by day of the year: one series per date ("08-15"), one period per year; totals are skipped. */
    private static StatisticsDataset calendar(String code, String table, int catalogId) {
        return CystatDataset.of(code, table, catalogId, json -> SeriesMapping.map(json, YEAR, Map.of(),
                List.of(MONTH, "DAY"), labels -> calendarDate(labels.get(0), labels.get(1))));
    }

    /** "August" and "15" become "08-15"; "Total" and "Not stated" have no date. */
    static Optional<String> calendarDate(String month, String day) {
        if (!day.matches("\\d{1,2}")) return Optional.empty();
        try {
            return Optional.of("%02d-%02d".formatted(Month.valueOf(month.toUpperCase(Locale.ROOT)).getValue(),
                    Integer.parseInt(day)));
        } catch (IllegalArgumentException notAMonth) {
            return Optional.empty();
        }
    }

    // Crime

    /** True cases of serious offences reported to the police, by offence group. */
    @Bean
    @Order(110)
    StatisticsDataset seriousOffences() {
        return CystatDataset.of("serious-offences", "Living Conditions, Social Protection/Crime/Police Statistics/1941010E.px",
                1933, table -> SeriesMapping.map(table, "OFFENCE GROUP AND VOLUME OF CRIME", labels(Map.ofEntries(
                        entry("Total number of true cases of serious offences reported", TOTAL_SERIES),
                        entry("Against public order", "public-order"),
                        entry("Against lawful authority", "lawful-authority"),
                        entry("Injurious to the public in general", "public-harm"),
                        entry("Sexual offences", "sexual"),
                        entry("Against the person (b)", "person"),
                        entry("Against property", "property"),
                        entry("Malicious damages to property", "damage"),
                        entry("Forgery, coining, counterfeiting, criminal impersonation etc.", "forgery"),
                        entry("Miscellaneous offences", "other"))),
                        YEAR, Map.of()));
    }

    // Earnings

    /** Average gross monthly earnings (€): mean and median, for all employees and by sex. */
    @Bean
    @Order(120)
    StatisticsDataset earnings() {
        var measures = Map.of("Mean", "mean", "Median", "median");
        var groups = Map.of("Total", TOTAL_SERIES, "Males", "men", "Females", "women");
        return CystatDataset.of("earnings", "Labour Cost and Earnings/Earnings/1110020E.px", 1773,
                table -> SeriesMapping.map(table, YEAR, Map.of(), List.of("MEASURE", "SEX"), labels ->
                        Optional.ofNullable(measures.get(labels.get(0))).flatMap(measure ->
                                Optional.ofNullable(groups.get(labels.get(1))).map(group -> measure + "-" + group))));
    }

    // Energy

    /** Monthly sales of petroleum products by fuel, in tonnes. */
    @Bean
    @Order(130)
    StatisticsDataset fuelSales() {
        return CystatDataset.of("fuel-sales", "Energy/0900010E.px", 1535, table -> SeriesMapping.map(
                table, "FUEL TYPE", labels(Map.ofEntries(
                        entry("Total fuels", TOTAL_SERIES),
                        entry("Unleaded motor gasoline 95 RON", "petrol-95"),
                        entry("Unleaded motor gasoline 98 RON", "petrol-98"),
                        entry("Road diesel", "diesel"),
                        entry("Aviation kerosene", "jet-fuel"),
                        entry("Heating gasoil", "heating-oil"),
                        entry("Gasoil for marine use", "marine-gasoil"),
                        entry("Heavy fuel oil", "heavy-fuel-oil"),
                        entry("Liquefied petroleum gases", "lpg"))),
                MONTH, Map.of("REFERENCE PERIOD", "Month", "SALES/STOCKS", "Total sales")));
    }

    /** CYSTAT appends notes in parentheses: "Bulgaria (It is included in the E.U. since 2007 )". */
    private static String withoutNotes(String label) {
        var note = label.indexOf(" (");
        return note < 0 ? label : label.substring(0, note);
    }
}
