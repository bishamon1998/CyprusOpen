package com.cyprusopen.places.beach;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BeachQualityTest {
    /** Every class EEA reports for Cyprus, as returned by Discodata on 7 November 2026. */
    @Test
    void turnsEeaClassesIntoApiKeys() {
        assertThat(BeachParser.quality("1 - Excellent")).contains("excellent");
        assertThat(BeachParser.quality("2 - Good")).contains("good");
        assertThat(BeachParser.quality("3 - Good or Sufficient")).contains("good-or-sufficient");
        assertThat(BeachParser.quality("4 - Poor")).contains("poor");
        assertThat(BeachParser.quality("0 - Not classified")).contains("not-classified");
        assertThat(BeachParser.quality(" 0 - Missing data ")).contains("missing-data");
        assertThat(BeachParser.quality("Excellent")).isEmpty();
        assertThat(BeachParser.quality("")).isEmpty();
    }
}
