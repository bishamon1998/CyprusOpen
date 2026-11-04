package com.cyprusopen.parking;

import java.net.URI;
import java.net.http.HttpClient;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

@Service
class DirectorySync {
    private final ParkingDirectory directory;
    private final DirectoryXmlParser parser;
    private final JdbcClient jdbc;
    private final TransactionTemplate transactions;
    private final RestClient client;
    private final URI resource;

    @Autowired
    DirectorySync(ParkingDirectory directory, DirectoryXmlParser parser, JdbcClient jdbc,
                  TransactionTemplate transactions, RestClient.Builder builder, Environment environment,
                  @Value("${parking.directory.resource-url}") URI resource) {
        this.directory = directory; this.parser = parser; this.jdbc = jdbc; this.transactions = transactions; this.resource = resource;
        var localTest = Arrays.asList(environment.getActiveProfiles()).contains("test")
                && "http".equals(resource.getScheme()) && java.util.Set.of("127.0.0.1", "parking-fixture").contains(resource.getHost());
        if (!localTest && !("https".equals(resource.getScheme()) && "fixcyprus.cy".equals(resource.getHost())
                && resource.getUserInfo() == null && resource.getPort() == -1)) throw new IllegalArgumentException("Unverified directory host");
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(Duration.ofSeconds(15));
        client = builder.requestFactory(factory).defaultHeader(HttpHeaders.USER_AGENT,
                "CyprusOpen/0.1 (+https://github.com/bishamon1998/CyprusOpen)").build();
    }

    public String refresh() {
        return transactions.execute(status -> {
            if (!jdbc.sql("SELECT pg_try_advisory_xact_lock(620041)").query(Boolean.class).single()) return "busy";
            if (!jdbc.sql("SELECT next_attempt_at <= current_timestamp AND consecutive_failures < 3 FROM parking_directory WHERE id=1").query(Boolean.class).single()) return "deferred";
            jdbc.sql("UPDATE parking_directory SET last_attempt_at=current_timestamp,next_attempt_at=current_timestamp+interval '5 seconds' WHERE id=1").update();
            try {
                var validators = jdbc.sql("SELECT etag,last_modified FROM parking_directory WHERE id=1").query((rs, row) -> new Validators(rs.getString(1), rs.getString(2))).single();
                return client.get().uri(resource).headers(headers -> {
                    if (validators.etag() != null) headers.set(HttpHeaders.IF_NONE_MATCH, validators.etag());
                    if (validators.modified() != null) headers.set(HttpHeaders.IF_MODIFIED_SINCE, validators.modified());
                }).exchange((request, response) -> {
                    if (response.getStatusCode().value() == 304) {
                        if (directory.snapshot().items().isEmpty()) {
                            failed(null);
                            return "invalid";
                        }
                        jdbc.sql("UPDATE parking_directory SET last_checked_at=current_timestamp,consecutive_failures=0,sync_status='unchanged' WHERE id=1").update();
                        return "unchanged";
                    }
                    if (response.getStatusCode().value() != 200) {
                        failed(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
                        return "unavailable";
                    }
                    var bytes = response.getBody().readNBytes(2 * 1024 * 1024 + 1);
                    if (bytes.length > 2 * 1024 * 1024) throw new IllegalArgumentException("Directory exceeds size limit");
                    try {
                        directory.replace(parser.parse(bytes, directory.snapshot(), Instant.now()),
                                response.getHeaders().getETag(), response.getHeaders().getFirst(HttpHeaders.LAST_MODIFIED));
                    } catch (Exception invalid) {
                        failed(null);
                        return "invalid";
                    }
                    return "updated";
                });
            } catch (Exception unavailable) {
                failed(null);
                return "unavailable";
            }
        });
    }

    private void failed(String retryAfter) {
        var next = Instant.now().plus(Duration.ofDays(1));
        if (retryAfter != null) {
            try {
                var requested = retryAfter.matches("\\d+") ? Instant.now().plusSeconds(Long.parseLong(retryAfter))
                        : ZonedDateTime.parse(retryAfter, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
                if (requested.isAfter(next)) next = requested;
            } catch (RuntimeException ignored) { /* An invalid header does not remove the conservative backoff. */ }
        }
        jdbc.sql("UPDATE parking_directory SET consecutive_failures=consecutive_failures+1,sync_status=CASE WHEN consecutive_failures>=2 THEN 'paused' ELSE 'error' END,next_attempt_at=:next WHERE id=1")
                .param("next", Timestamp.from(next)).update();
    }

    private record Validators(String etag, String modified) {}
}
