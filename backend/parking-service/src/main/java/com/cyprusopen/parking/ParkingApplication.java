package com.cyprusopen.parking;

import static com.cyprusopen.parking.DirectoryConstants.MAX_RESPONSE_BYTES;

import com.cyprusopen.common.xml.XmlDocumentReader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ParkingApplication {
    @Bean
    XmlDocumentReader xmlDocumentReader() {
        return new XmlDocumentReader(MAX_RESPONSE_BYTES);
    }

    public static void main(String[] args) {
        SpringApplication.run(ParkingApplication.class, args);
    }
}
