package com.cyprusopen.parking;

import static com.cyprusopen.parking.DirectoryConstants.CONNECT_TIMEOUT;
import static com.cyprusopen.parking.DirectoryConstants.MAX_RESPONSE_BYTES;
import static com.cyprusopen.parking.DirectoryConstants.READ_TIMEOUT;

import com.cyprusopen.common.http.Upstream;
import java.net.URI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class DirectoryClient {
    private final RestClient client;
    private final URI resource;

    @Autowired
    DirectoryClient(RestClient.Builder builder, @Value("${parking.directory.resource-url}") URI resource) {
        this.resource = Upstream.requireHttpEndpoint(resource);
        client = Upstream.restClient(builder, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    Response fetch(ParkingDirectory.Validators validators) {
        return client.get().uri(resource).headers(headers -> {
            if (validators.etag() != null) headers.set(HttpHeaders.IF_NONE_MATCH, validators.etag());
            if (validators.modified() != null) headers.set(HttpHeaders.IF_MODIFIED_SINCE, validators.modified());
        }).exchange((request, response) -> {
            var body = response.getStatusCode().value() == HttpStatus.OK.value()
                    ? Upstream.readLimited(response.getBody(), MAX_RESPONSE_BYTES) : new byte[0];
            var headers = response.getHeaders();
            return new Response(response.getStatusCode().value(), body, headers.getETag(),
                    headers.getFirst(HttpHeaders.LAST_MODIFIED));
        });
    }

    record Response(int status, byte[] body, String etag, String modified) {}
}
