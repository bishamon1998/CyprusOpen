package com.cyprusopen.places.parking;

import static com.cyprusopen.places.parking.DirectoryConstants.CONNECT_TIMEOUT;
import static com.cyprusopen.places.parking.DirectoryConstants.MAX_RESPONSE_BYTES;
import static com.cyprusopen.places.parking.DirectoryConstants.READ_TIMEOUT;

import com.cyprusopen.common.http.Upstream;
import java.net.URI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Conditional GET of the configured DATEX II directory resource. */
@Component
class DirectoryClient {
    private final RestClient client;
    private final URI resource;

    @Autowired
    DirectoryClient(RestClient.Builder builder, @Value("${parking.directory.resource-url}") URI resource) {
        this.resource = Upstream.requireHttpEndpoint(resource);
        client = Upstream.restClient(builder, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    Upstream.Response fetch(ParkingDirectory.Validators validators) {
        return Upstream.fetch(client.get().uri(resource).headers(headers -> {
            if (validators.etag() != null) headers.set(HttpHeaders.IF_NONE_MATCH, validators.etag());
            if (validators.modified() != null) headers.set(HttpHeaders.IF_MODIFIED_SINCE, validators.modified());
        }), MAX_RESPONSE_BYTES);
    }
}
