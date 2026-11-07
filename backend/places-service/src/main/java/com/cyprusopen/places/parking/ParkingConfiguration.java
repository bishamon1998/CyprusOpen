package com.cyprusopen.places.parking;

import static com.cyprusopen.places.parking.DirectoryConstants.MAX_RESPONSE_BYTES;

import com.cyprusopen.common.xml.XmlDocumentReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Parking directory beans: the bounded XML reader for the DATEX II publication. */
@Configuration
class ParkingConfiguration {
    @Bean
    XmlDocumentReader xmlDocumentReader() {
        return new XmlDocumentReader(MAX_RESPONSE_BYTES);
    }
}
