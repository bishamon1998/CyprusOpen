package com.cyprusopen.places.air;

import static com.cyprusopen.places.air.AirQualityConstants.HISTORY;
import static com.cyprusopen.places.air.AirQualityConstants.REFRESH_AFTER;

import com.cyprusopen.common.refresh.RefreshJob;
import com.cyprusopen.common.refresh.RefreshResult;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/**
 * Hourly import of the current station readings; a failure keeps the previous snapshot. Each import also adds the
 * readings to the week of history, which is trimmed by the newest reading so a stalled source keeps its last week.
 */
@Service
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class AirQualitySync implements RefreshJob {
    private final AirQualityClient client;
    private final AirQualityParser parser;
    private final AirQualityStore store;
    private final AirQualityHistory history;
    private final Clock clock;

    @Override
    public String name() {
        return "air-quality";
    }

    @Override
    public boolean needsInitialRun() {
        return store.olderThan(REFRESH_AFTER, clock.instant());
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
            var snapshot = parser.parse(body, clock.instant());
            store.replace(snapshot, snapshot.retrievedAt());
            history.record(snapshot);
            snapshot.stations().stream().map(AirQualitySnapshot.Station::measuredAt).max(Instant::compareTo)
                    .ifPresent(newest -> history.removeBefore(newest.minus(HISTORY)));
            return RefreshResult.UPDATED;
        } catch (IllegalArgumentException invalid) {
            log.warn("Air quality response rejected: {}", invalid.getMessage());
            return RefreshResult.INVALID;
        }
    }
}
