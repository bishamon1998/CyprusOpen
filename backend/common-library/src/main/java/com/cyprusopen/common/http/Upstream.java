package com.cyprusopen.common.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Shared safeguards for requests to official upstream sources. */
public final class Upstream {
    public static final String USER_AGENT = "CyprusOpen/0.1 (+https://github.com/bishamon1998/CyprusOpen)";
    private static final Set<String> HTTP_SCHEMES = Set.of("http", "https");

    private Upstream() {}

    /**
     * A client with connect/read timeouts, no redirects and the project user agent. The read timeout covers the
     * whole exchange, including reading the body.
     */
    public static RestClient restClient(RestClient.Builder builder, Duration connectTimeout, Duration readTimeout) {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(connectTimeout).followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(readTimeout);
        return builder.requestFactory(factory).defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT).build();
    }

    /** Rejects configured endpoints that are relative, non-HTTP, carry credentials or a fragment. */
    public static URI requireHttpEndpoint(URI endpoint) {
        if (!endpoint.isAbsolute() || !HTTP_SCHEMES.contains(endpoint.getScheme()) || endpoint.getHost() == null
                || endpoint.getUserInfo() != null || endpoint.getFragment() != null) {
            throw new IllegalArgumentException("Invalid upstream endpoint configuration");
        }
        return endpoint;
    }

    /** Reads at most {@code maxBytes}; larger bodies are rejected instead of truncated. */
    public static byte[] readLimited(InputStream body, int maxBytes) throws IOException {
        var bytes = body.readNBytes(maxBytes + 1);
        if (bytes.length > maxBytes) throw new IllegalArgumentException("Upstream response exceeds size limit");
        return bytes;
    }

    /**
     * Sends the request and reads the body only for 200 OK, within {@code maxBytes}. Other statuses (including
     * redirects and 304 Not Modified) return an empty body. Network failures surface as
     * {@link org.springframework.web.client.RestClientException}; an oversized body as
     * {@link IllegalArgumentException}.
     */
    public static Response fetch(RestClient.RequestHeadersSpec<?> request, int maxBytes) {
        return request.exchange((sent, response) -> {
            var status = response.getStatusCode().value();
            var body = status == HttpStatus.OK.value() ? readLimited(response.getBody(), maxBytes) : new byte[0];
            var headers = response.getHeaders();
            return new Response(status, body, headers.getETag(), headers.getFirst(HttpHeaders.LAST_MODIFIED));
        });
    }

    /** An upstream reply with the validators needed for the next conditional request. */
    public record Response(int status, byte[] body, String etag, String lastModified) {
        public boolean ok() {
            return status == HttpStatus.OK.value();
        }
    }
}
