package com.cyprusopen.statistics;

import static com.cyprusopen.statistics.StatisticsConstants.CONNECT_TIMEOUT;
import static com.cyprusopen.statistics.StatisticsConstants.FULL_TABLE_QUERY;
import static com.cyprusopen.statistics.StatisticsConstants.MAX_RESPONSE_BYTES;
import static com.cyprusopen.statistics.StatisticsConstants.RATE_LIMIT_ATTEMPTS;
import static com.cyprusopen.statistics.StatisticsConstants.READ_TIMEOUT;

import com.cyprusopen.common.http.Upstream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriUtils;

/**
 * Downloads whole tables from the CYSTAT PxWeb API. Tables are addressed relative to the configured API root.
 * CYSTAT rejects bursts of requests (HTTP 429), so requests are spaced out and a rejected one is retried later.
 */
@Component
class CystatClient {
    private final RestClient client;
    private final URI api;
    private final Duration interval;
    private long nextRequestAt;

    @Autowired
    CystatClient(RestClient.Builder builder, @Value("${statistics.cystat-api-url}") URI api,
                 @Value("${statistics.cystat-request-interval}") Duration interval) {
        this.api = Upstream.requireHttpEndpoint(api);
        this.interval = interval;
        client = Upstream.restClient(builder, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    synchronized byte[] table(String path) {
        // Table paths contain spaces ("Price Indices/..."), so each segment is percent-encoded.
        var table = api.resolve(UriUtils.encodePath(path, StandardCharsets.UTF_8));
        if (!table.toString().startsWith(api.toString())) {
            throw new IllegalArgumentException("Table is outside the API root");
        }
        for (var attempt = 1; ; attempt++) {
            waitForTurn(attempt);
            var response = client.post().uri(table).contentType(MediaType.APPLICATION_JSON).body(FULL_TABLE_QUERY)
                    .exchange((request, reply) -> new Reply(reply.getStatusCode().value(),
                            reply.getStatusCode().value() == HttpStatus.OK.value()
                                    ? Upstream.readLimited(reply.getBody(), MAX_RESPONSE_BYTES) : null));
            if (response.status() == HttpStatus.OK.value()) return response.body();
            if (response.status() != HttpStatus.TOO_MANY_REQUESTS.value() || attempt == RATE_LIMIT_ATTEMPTS) {
                throw new RestClientException("CYSTAT returned " + response.status());
            }
        }
    }

    /** Keeps {@code interval} between requests; after a rejection waits longer on each retry. */
    private void waitForTurn(int attempt) {
        var wait = nextRequestAt - System.nanoTime();
        if (attempt > 1) wait = Math.max(wait, interval.toNanos() * 4 * (attempt - 1));
        if (wait > 0) {
            try {
                Thread.sleep(Duration.ofNanos(wait));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new RestClientException("Interrupted while waiting for CYSTAT");
            }
        }
        nextRequestAt = System.nanoTime() + interval.toNanos();
    }

    private record Reply(int status, byte[] body) {}
}
