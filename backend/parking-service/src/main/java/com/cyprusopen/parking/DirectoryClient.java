package com.cyprusopen.parking;

import static com.cyprusopen.parking.DirectoryConstants.CONNECT_TIMEOUT;
import static com.cyprusopen.parking.DirectoryConstants.HTTP_SCHEMES;
import static com.cyprusopen.parking.DirectoryConstants.MAX_RESPONSE_BYTES;
import static com.cyprusopen.parking.DirectoryConstants.READ_TIMEOUT;
import static com.cyprusopen.parking.DirectoryConstants.USER_AGENT;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class DirectoryClient {
    private final RestClient client;
    private final URI resource;

    @Autowired
    DirectoryClient(RestClient.Builder builder, @Value("${parking.directory.resource-url}") URI resource) {
        if (!resource.isAbsolute() || !HTTP_SCHEMES.contains(resource.getScheme()) || resource.getHost() == null
                || resource.getUserInfo() != null || resource.getFragment() != null) {
            throw new IllegalArgumentException("Invalid directory endpoint configuration");
        }
        this.resource = resource;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT).followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(READ_TIMEOUT);
        client = builder.requestFactory(factory).defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT).build();
    }

    Response fetch(ParkingDirectory.Validators validators) {
        return client.get().uri(resource).headers(headers -> {
            if (validators.etag() != null) headers.set(HttpHeaders.IF_NONE_MATCH, validators.etag());
            if (validators.modified() != null) headers.set(HttpHeaders.IF_MODIFIED_SINCE, validators.modified());
        }).exchange((request, response) -> {
            var body = response.getStatusCode().value() == HttpStatus.OK.value()
                    ? response.getBody().readNBytes(MAX_RESPONSE_BYTES + 1) : new byte[0];
            if (body.length > MAX_RESPONSE_BYTES) throw new IllegalArgumentException("Directory exceeds size limit");
            var headers = response.getHeaders();
            return new Response(response.getStatusCode().value(), body, headers.getETag(),
                    headers.getFirst(HttpHeaders.LAST_MODIFIED), retryAt(headers.getFirst(HttpHeaders.RETRY_AFTER)));
        });
    }

    private static Instant retryAt(String value) {
        if (value == null) return null;
        try {
            return value.matches("\\d+") ? Instant.now().plusSeconds(Long.parseLong(value))
                    : ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (RuntimeException invalidHeader) {
            return null;
        }
    }

    record Response(int status, byte[] body, String etag, String modified, Instant retryAt) {}
}
