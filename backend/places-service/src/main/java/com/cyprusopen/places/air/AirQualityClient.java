package com.cyprusopen.places.air;

import static com.cyprusopen.places.air.AirQualityConstants.CONNECT_TIMEOUT;
import static com.cyprusopen.places.air.AirQualityConstants.MAX_RESPONSE_BYTES;
import static com.cyprusopen.places.air.AirQualityConstants.READ_TIMEOUT;

import com.cyprusopen.common.http.Upstream;
import java.net.URI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Downloads the current readings of all stations from the configured official endpoint. */
@Component
class AirQualityClient {
    private final RestClient client;
    private final URI resource;

    @Autowired
    AirQualityClient(RestClient.Builder builder, @Value("${air-quality.resource-url}") URI resource) {
        this.resource = Upstream.requireHttpEndpoint(resource);
        client = Upstream.restClient(builder, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    byte[] fetch() {
        var response = Upstream.fetch(client.get().uri(resource), MAX_RESPONSE_BYTES);
        if (!response.ok()) throw new RestClientException("Air quality source returned " + response.status());
        return response.body();
    }
}
