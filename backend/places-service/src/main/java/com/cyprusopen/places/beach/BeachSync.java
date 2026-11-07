package com.cyprusopen.places.beach;

import static com.cyprusopen.places.beach.BeachConstants.QUALITY;
import static com.cyprusopen.places.beach.BeachConstants.REFRESH_AFTER;
import static com.cyprusopen.places.beach.BeachConstants.SPATIAL;
import static com.cyprusopen.places.beach.BeachConstants.STATUS;

import com.cyprusopen.common.refresh.RefreshJob;
import com.cyprusopen.common.refresh.RefreshResult;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/** Weekly import of bathing waters, classifications and sample statistics; a failure keeps the previous snapshot. */
@Service
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class BeachSync implements RefreshJob {
    private final BeachClient client;
    private final BeachParser parser;
    private final BeachStore store;
    private final Clock clock;

    @Override
    public String name() {
        return "beaches";
    }

    @Override
    public boolean needsInitialRun() {
        return store.retrievedAt().map(retrieved -> retrieved.plus(REFRESH_AFTER).isBefore(clock.instant())).orElse(true);
    }

    @Override
    public RefreshResult refresh() {
        byte[] spatial;
        byte[] status;
        byte[] quality;
        try {
            spatial = client.table(SPATIAL);
            status = client.table(STATUS);
            quality = client.table(QUALITY);
        } catch (RestClientException | IllegalArgumentException unavailable) {
            log.warn("Bathing water source unavailable: {}", unavailable.getMessage());
            return RefreshResult.UNAVAILABLE;
        }
        try {
            store.replace(parser.parse(spatial, status, quality, clock.instant()));
            return RefreshResult.UPDATED;
        } catch (IllegalArgumentException invalid) {
            log.warn("Bathing water response rejected: {}", invalid.getMessage());
            return RefreshResult.INVALID;
        }
    }
}
