package com.cyprusopen.places.beach;

import static com.cyprusopen.places.beach.BeachConstants.CONNECT_TIMEOUT;
import static com.cyprusopen.places.beach.BeachConstants.COUNTRY;
import static com.cyprusopen.places.beach.BeachConstants.MAX_RESPONSE_BYTES;
import static com.cyprusopen.places.beach.BeachConstants.READ_TIMEOUT;

import com.cyprusopen.common.http.Upstream;
import java.net.URI;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Reads whole Bathing Water Directive tables for Cyprus from the EEA Discodata SQL endpoint. Queries are fixed
 * "SELECT *" statements: the endpoint's firewall rejects some statements that list columns or aggregate.
 */
@Component
class BeachClient {
    private final RestClient client;
    private final URI endpoint;

    @Autowired
    BeachClient(RestClient.Builder builder, @Value("${beaches.discodata-url}") URI endpoint) {
        this.endpoint = Upstream.requireHttpEndpoint(endpoint);
        client = Upstream.restClient(builder, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    byte[] table(String table) {
        var query = "SELECT * FROM [WISE_BWD].[latest].[" + table + "] WHERE countryCode='" + COUNTRY + "'";
        var uri = UriComponentsBuilder.fromUri(endpoint)
                .queryParam("query", "{query}").queryParam("p", 1).queryParam("nrOfHits", 100_000)
                .encode().buildAndExpand(Map.of("query", query)).toUri();
        return client.get().uri(uri).exchange((request, response) -> {
            if (response.getStatusCode().value() != HttpStatus.OK.value()) {
                throw new RestClientException("EEA Discodata returned " + response.getStatusCode().value());
            }
            return Upstream.readLimited(response.getBody(), MAX_RESPONSE_BYTES);
        });
    }
}
