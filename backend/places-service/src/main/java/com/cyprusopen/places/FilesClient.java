package com.cyprusopen.places;

import com.cyprusopen.common.http.Upstream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Downloads files by fixed, already URL-encoded names from one folder, such as data.gov.cy's files folder. */
public final class FilesClient {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    private final RestClient client;
    private final URI folder;
    private final int maxBytes;

    public FilesClient(RestClient.Builder builder, URI folder, int maxBytes) {
        this.folder = Upstream.requireHttpEndpoint(folder);
        this.maxBytes = maxBytes;
        client = Upstream.restClient(builder, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    /** {@code file} is a constant of the caller, never user input; a status other than 200 throws. */
    public Upstream.Response file(String file) {
        var response = Upstream.fetch(client.get().uri(folder.resolve(file)), maxBytes);
        if (!response.ok()) throw new RestClientException(file + " returned " + response.status());
        return response;
    }

    /** When the source last changed any of the files; null when it sends no Last-Modified. */
    public static Instant latest(List<Upstream.Response> responses) {
        return responses.stream().map(Upstream.Response::lastModifiedAt).filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(null);
    }
}
