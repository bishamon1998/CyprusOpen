package com.cyprusopen.common.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Set;

/** Shared safeguards for requests to official upstream sources. */
public final class Upstream {
    public static final String USER_AGENT = "CyprusOpen/0.1 (+https://github.com/bishamon1998/CyprusOpen)";
    private static final Set<String> HTTP_SCHEMES = Set.of("http", "https");

    private Upstream() {}

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
