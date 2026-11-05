package com.cyprusopen.pharmacies;

import static com.cyprusopen.pharmacies.PharmacyConstants.CONNECT_TIMEOUT;
import static com.cyprusopen.pharmacies.PharmacyConstants.MAX_RESPONSE_BYTES;
import static com.cyprusopen.pharmacies.PharmacyConstants.READ_TIMEOUT;

import com.cyprusopen.common.http.Upstream;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Downloads catalog metadata and district files, only from the configured catalog origin. */
@Component
class ScheduleClient {
    private final RestClient client;
    private final URI metadata;

    @Autowired
    ScheduleClient(RestClient.Builder builder, @Value("${pharmacies.catalog-metadata-url}") URI metadata) {
        this.metadata = Upstream.requireHttpEndpoint(metadata);
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT).followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(READ_TIMEOUT);
        client = builder.requestFactory(factory).defaultHeader(HttpHeaders.USER_AGENT, Upstream.USER_AGENT).build();
    }

    String metadata() {
        return download(metadata);
    }

    String download(URI resource) {
        if (!metadata.getScheme().equals(resource.getScheme()) || !metadata.getHost().equals(resource.getHost())
                || metadata.getPort() != resource.getPort() || resource.getUserInfo() != null) {
            throw new RestClientException("Unexpected pharmacy schedule origin");
        }
        return client.get().uri(resource).exchange((request, response) -> {
            if (response.getStatusCode().value() != HttpStatus.OK.value()) {
                throw new RestClientException("Source returned " + response.getStatusCode().value());
            }
            return new String(Upstream.readLimited(response.getBody(), MAX_RESPONSE_BYTES), StandardCharsets.UTF_8);
        });
    }
}
