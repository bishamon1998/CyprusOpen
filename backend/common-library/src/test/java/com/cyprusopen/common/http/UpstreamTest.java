package com.cyprusopen.common.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

class UpstreamTest {
    private static final Duration TIMEOUT = Duration.ofMillis(500);

    private HttpServer server;
    private final ExecutorService handlers = Executors.newCachedThreadPool();
    private final AtomicInteger redirectTargetHits = new AtomicInteger();
    private final AtomicReference<String> userAgent = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ok", exchange -> {
            userAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
            var body = "hello".getBytes();
            exchange.getResponseHeaders().add("ETag", "\"v1\"");
            exchange.getResponseHeaders().add("Last-Modified", "Wed, 04 Nov 2026 00:00:00 GMT");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "/target");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/target", exchange -> {
            redirectTargetHits.incrementAndGet();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.createContext("/slow-body", exchange -> {
            exchange.sendResponseHeaders(200, 1_000);
            exchange.getResponseBody().write(new byte[10]);
            exchange.getResponseBody().flush();
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException stopped) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });
        server.setExecutor(handlers);
        server.start();
    }

    @AfterEach
    void stopServer() {
        handlers.shutdownNow();
        server.stop(0);
    }

    @Test
    void acceptsOnlyAbsoluteHttpEndpointsWithoutCredentialsOrFragment() {
        assertThat(Upstream.requireHttpEndpoint(URI.create("https://example.org/data"))).hasHost("example.org");
        assertThat(Upstream.requireHttpEndpoint(URI.create("http://127.0.0.1:8080/data"))).hasPort(8080);
        for (var invalid : new String[] {"/relative", "ftp://example.org/data", "https://user:secret@example.org/",
                "https://example.org/data#part", "file:///etc/passwd", "https:///no-host"}) {
            assertThatThrownBy(() -> Upstream.requireHttpEndpoint(URI.create(invalid))).as(invalid)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void rejectsBodiesOverTheLimitInsteadOfTruncating() throws IOException {
        assertThat(Upstream.readLimited(new ByteArrayInputStream(new byte[8]), 8)).hasSize(8);
        assertThatThrownBy(() -> Upstream.readLimited(new ByteArrayInputStream(new byte[9]), 8))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sendsTheProjectUserAgentAndNeverFollowsRedirects() {
        var client = Upstream.restClient(RestClient.builder(), TIMEOUT, TIMEOUT);
        assertThat(client.get().uri(url("/ok")).retrieve().body(String.class)).isEqualTo("hello");
        assertThat(userAgent.get()).isEqualTo(Upstream.USER_AGENT);

        var status = client.get().uri(url("/redirect"))
                .exchange((request, response) -> response.getStatusCode().value());
        assertThat(status).isEqualTo(302);
        assertThat(redirectTargetHits).hasValue(0);
    }

    @Test
    void fetchReadsOnlySuccessfulBodiesWithinTheLimitAndKeepsValidators() {
        var client = Upstream.restClient(RestClient.builder(), TIMEOUT, TIMEOUT);
        var ok = Upstream.fetch(client.get().uri(url("/ok")), 5);
        assertThat(ok.ok()).isTrue();
        assertThat(ok.body()).asString().isEqualTo("hello");
        assertThat(ok.etag()).isEqualTo("\"v1\"");
        assertThat(ok.lastModified()).isEqualTo("Wed, 04 Nov 2026 00:00:00 GMT");

        var redirect = Upstream.fetch(client.get().uri(url("/redirect")), 5);
        assertThat(redirect.ok()).isFalse();
        assertThat(redirect.status()).isEqualTo(302);
        assertThat(redirect.body()).isEmpty();
        assertThat(redirectTargetHits).hasValue(0);

        assertThatThrownBy(() -> Upstream.fetch(client.get().uri(url("/ok")), 4))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** A source that sends headers and then stalls must not block the single refresh thread. */
    @Test
    void readTimeoutAlsoBoundsASlowBody() {
        var client = Upstream.restClient(RestClient.builder(), TIMEOUT, TIMEOUT);
        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> assertThatThrownBy(() -> client.get()
                .uri(url("/slow-body"))
                .exchange((request, response) -> Upstream.readLimited(response.getBody(), 1_000)))
                .isInstanceOf(RestClientException.class));
    }

    private URI url(String path) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }
}
