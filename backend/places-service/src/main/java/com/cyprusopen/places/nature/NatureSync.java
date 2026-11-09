package com.cyprusopen.places.nature;

import static com.cyprusopen.places.nature.NatureConstants.CAMPING_CATALOGUE_FILE;
import static com.cyprusopen.places.nature.NatureConstants.CAMPING_FILE;
import static com.cyprusopen.places.nature.NatureConstants.PICNIC_FILE;
import static com.cyprusopen.places.nature.NatureConstants.REFRESH_AFTER;
import static com.cyprusopen.places.nature.NatureConstants.TRAIL_CATALOGUE_FILE;
import static com.cyprusopen.places.nature.NatureConstants.TRAIL_ROUTES_FILE;
import static com.cyprusopen.places.nature.NatureConstants.TRAIL_STARTS_FILE;

import com.cyprusopen.common.refresh.RefreshJob;
import com.cyprusopen.common.refresh.RefreshResult;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/** Monthly import of picnic sites, camping sites and nature trails; a failure keeps the previous snapshot. */
@Service
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class NatureSync implements RefreshJob {
    private final NatureClient client;
    private final NatureParser parser;
    private final NatureStore store;
    private final Clock clock;

    @Override
    public String name() {
        return "nature";
    }

    @Override
    public boolean needsInitialRun() {
        return store.olderThan(REFRESH_AFTER, clock.instant());
    }

    @Override
    public RefreshResult refresh() {
        NatureParser.Files files;
        try {
            files = new NatureParser.Files(client.file(PICNIC_FILE), client.file(CAMPING_FILE),
                    client.file(CAMPING_CATALOGUE_FILE), client.file(TRAIL_ROUTES_FILE),
                    client.file(TRAIL_STARTS_FILE), client.file(TRAIL_CATALOGUE_FILE));
        } catch (RestClientException | IllegalArgumentException unavailable) {
            log.warn("Department of Forests files unavailable: {}", unavailable.getMessage());
            return RefreshResult.UNAVAILABLE;
        }
        try {
            var snapshot = parser.parse(files, clock.instant());
            store.replace(snapshot, snapshot.retrievedAt());
            return RefreshResult.UPDATED;
        } catch (IllegalArgumentException invalid) {
            log.warn("Department of Forests files rejected: {}", invalid.getMessage());
            return RefreshResult.INVALID;
        }
    }
}
