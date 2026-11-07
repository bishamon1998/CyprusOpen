package com.cyprusopen.places.air;

import static com.cyprusopen.places.air.AirQualityConstants.REFRESH_AFTER;

import com.cyprusopen.common.refresh.RefreshJob;
import com.cyprusopen.common.refresh.RefreshResult;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/** Hourly import of the current station readings; a failure keeps the previous snapshot. */
@Service
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class AirQualitySync implements RefreshJob {
    private final AirQualityClient client;
    private final AirQualityParser parser;
    private final AirQualityStore store;
    private final Clock clock;

    @Override
    public String name() {
        return "air-quality";
    }

    @Override
    public boolean needsInitialRun() {
        return store.retrievedAt().map(retrieved -> retrieved.plus(REFRESH_AFTER).isBefore(clock.instant())).orElse(true);
    }

    @Override
    public RefreshResult refresh() {
        byte[] body;
        try {
            body = client.fetch();
        } catch (RestClientException | IllegalArgumentException unavailable) {
            log.warn("Air quality source unavailable: {}", unavailable.getMessage());
            return RefreshResult.UNAVAILABLE;
        }
        try {
            store.replace(parser.parse(body, clock.instant()));
            return RefreshResult.UPDATED;
        } catch (IllegalArgumentException invalid) {
            log.warn("Air quality response rejected: {}", invalid.getMessage());
            return RefreshResult.INVALID;
        }
    }
}
