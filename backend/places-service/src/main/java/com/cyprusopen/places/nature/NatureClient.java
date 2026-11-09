package com.cyprusopen.places.nature;

import static com.cyprusopen.places.nature.NatureConstants.CONNECT_TIMEOUT;
import static com.cyprusopen.places.nature.NatureConstants.MAX_FILE_BYTES;
import static com.cyprusopen.places.nature.NatureConstants.READ_TIMEOUT;

import com.cyprusopen.common.http.Upstream;
import java.net.URI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Downloads Department of Forests files by their fixed names from the configured data.gov.cy files folder. */
@Component
class NatureClient {
    private final RestClient client;
    private final URI folder;

    @Autowired
    NatureClient(RestClient.Builder builder, @Value("${nature.files-url}") URI folder) {
        this.folder = Upstream.requireHttpEndpoint(folder);
        client = Upstream.restClient(builder, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    /** {@code file} is one of the constant names in {@link NatureConstants}, never user input. */
    Upstream.Response file(String file) {
        var response = Upstream.fetch(client.get().uri(folder.resolve(file)), MAX_FILE_BYTES);
        if (!response.ok()) throw new RestClientException(file + " returned " + response.status());
        return response;
    }
}
