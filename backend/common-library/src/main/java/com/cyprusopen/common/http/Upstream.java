package com.cyprusopen.common.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Shared safeguards for requests to official upstream sources. */
public final class Upstream {
    public static final String USER_AGENT = "CyprusOpen/0.1 (+https://github.com/bishamon1998/CyprusOpen)";
    private static final Set<String> HTTP_SCHEMES = Set.of("http", "https");

    private Upstream() {}

    /** A client with connect/read timeouts, no redirects and the project user agent. */
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
}
