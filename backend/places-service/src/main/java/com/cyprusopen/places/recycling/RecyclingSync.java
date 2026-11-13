package com.cyprusopen.places.recycling;

import static com.cyprusopen.places.recycling.RecyclingConstants.CYPRUS;
import static com.cyprusopen.places.recycling.RecyclingConstants.EXPORTED_ON;
import static com.cyprusopen.places.recycling.RecyclingConstants.MAX_FILE_BYTES;
import static com.cyprusopen.places.recycling.RecyclingConstants.POINTS_FILE;
import static com.cyprusopen.places.recycling.RecyclingConstants.REFRESH_AFTER;

import com.cyprusopen.common.http.Upstream;
import com.cyprusopen.common.refresh.RefreshJob;
import com.cyprusopen.common.refresh.RefreshResult;
import com.cyprusopen.places.FilesClient;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Monthly import of Green Dot recycling points; a failure keeps the previous snapshot. */
@Service
@Slf4j
class RecyclingSync implements RefreshJob {
    private final FilesClient client;
    private final RecyclingParser parser;
    private final RecyclingStore store;
    private final Clock clock;

    @Autowired
    RecyclingSync(RestClient.Builder builder, @Value("${recycling.files-url}") URI folder, RecyclingParser parser,
                  RecyclingStore store, Clock clock) {
        client = new FilesClient(builder, folder, MAX_FILE_BYTES);
        this.parser = parser;
        this.store = store;
        this.clock = clock;
    }

    @Override
    public String name() {
        return "recycling";
    }

    @Override
    public boolean needsInitialRun() {
        return store.olderThan(REFRESH_AFTER, clock.instant());
    }

    @Override
    public RefreshResult refresh() {
        Upstream.Response file;
        try {
            file = client.file(POINTS_FILE);
        } catch (RestClientException | IllegalArgumentException unavailable) {
            log.warn("Green Dot points file unavailable: {}", unavailable.getMessage());
            return RefreshResult.UNAVAILABLE;
        }
        try {
            var snapshot = parser.parse(file.body(), publishedAt(file), clock.instant());
            store.replace(snapshot, snapshot.retrievedAt());
            return RefreshResult.UPDATED;
        } catch (IllegalArgumentException invalid) {
            log.warn("Green Dot points file rejected: {}", invalid.getMessage());
            return RefreshResult.INVALID;
        }
    }

    /** The export date of the file, or its Last-Modified when that is older. */
    private static Instant publishedAt(Upstream.Response file) {
        var exported = EXPORTED_ON.atStartOfDay(CYPRUS).toInstant();
        var modified = file.lastModifiedAt();
        return modified != null && modified.isBefore(exported) ? modified : exported;
    }
}
