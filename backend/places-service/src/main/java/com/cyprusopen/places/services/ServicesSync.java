package com.cyprusopen.places.services;

import static com.cyprusopen.places.services.ServicesConstants.CENTRES_FILE;
import static com.cyprusopen.places.services.ServicesConstants.HOSPITALS_FILE;
import static com.cyprusopen.places.services.ServicesConstants.MAX_FILE_BYTES;
import static com.cyprusopen.places.services.ServicesConstants.REFRESH_AFTER;

import com.cyprusopen.common.refresh.RefreshJob;
import com.cyprusopen.common.refresh.RefreshResult;
import com.cyprusopen.places.FilesClient;
import java.net.URI;
import java.time.Clock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Monthly import of state hospitals and Citizen Service Centres; a failure keeps the previous snapshot. */
@Service
@Slf4j
class ServicesSync implements RefreshJob {
    private final FilesClient client;
    private final ServicesParser parser;
    private final ServicesStore store;
    private final Clock clock;

    @Autowired
    ServicesSync(RestClient.Builder builder, @Value("${services.files-url}") URI folder, ServicesParser parser,
                 ServicesStore store, Clock clock) {
        client = new FilesClient(builder, folder, MAX_FILE_BYTES);
        this.parser = parser;
        this.store = store;
        this.clock = clock;
    }

    @Override
    public String name() {
        return "services";
    }

    @Override
    public boolean needsInitialRun() {
        return store.olderThan(REFRESH_AFTER, clock.instant());
    }

    @Override
    public RefreshResult refresh() {
        ServicesParser.Files files;
        try {
            var hospitals = client.file(HOSPITALS_FILE);
            var centres = client.file(CENTRES_FILE);
            files = new ServicesParser.Files(hospitals.body(), centres.body(), hospitals.lastModifiedAt(),
                    centres.lastModifiedAt());
        } catch (RestClientException | IllegalArgumentException unavailable) {
            log.warn("Hospital and Citizen Service Centre files unavailable: {}", unavailable.getMessage());
            return RefreshResult.UNAVAILABLE;
        }
        try {
            var snapshot = parser.parse(files, clock.instant());
            store.replace(snapshot, snapshot.retrievedAt());
            return RefreshResult.UPDATED;
        } catch (IllegalArgumentException invalid) {
            log.warn("Hospital and Citizen Service Centre files rejected: {}", invalid.getMessage());
            return RefreshResult.INVALID;
        }
    }
}
