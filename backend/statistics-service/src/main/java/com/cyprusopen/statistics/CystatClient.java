package com.cyprusopen.statistics;

import static com.cyprusopen.statistics.StatisticsConstants.CONNECT_TIMEOUT;
import static com.cyprusopen.statistics.StatisticsConstants.FULL_TABLE_QUERY;
import static com.cyprusopen.statistics.StatisticsConstants.MAX_RESPONSE_BYTES;
import static com.cyprusopen.statistics.StatisticsConstants.READ_TIMEOUT;

import com.cyprusopen.common.http.Upstream;
import java.net.URI;
import java.net.http.HttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Downloads whole tables from the CYSTAT PxWeb API. Tables are addressed relative to the configured API root. */
@Component
class CystatClient {
    private final RestClient client;
    private final URI api;

    @Autowired
    CystatClient(RestClient.Builder builder, @Value("${statistics.cystat-api-url}") URI api) {
        this.api = Upstream.requireHttpEndpoint(api);
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT).followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(READ_TIMEOUT);
        client = builder.requestFactory(factory).defaultHeader(HttpHeaders.USER_AGENT, Upstream.USER_AGENT).build();
    }

    byte[] table(String path) {
        var table = api.resolve(path);
        if (!table.toString().startsWith(api.toString())) {
            throw new IllegalArgumentException("Table is outside the API root");
        }
        return client.post().uri(table).contentType(MediaType.APPLICATION_JSON).body(FULL_TABLE_QUERY)
                .exchange((request, response) -> {
                    if (response.getStatusCode().value() != HttpStatus.OK.value()) {
                        throw new RestClientException("CYSTAT returned " + response.getStatusCode().value());
                    }
                    return Upstream.readLimited(response.getBody(), MAX_RESPONSE_BYTES);
                });
    }
}
