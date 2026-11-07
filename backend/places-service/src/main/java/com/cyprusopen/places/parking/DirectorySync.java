package com.cyprusopen.places.parking;

import com.cyprusopen.common.http.Upstream;
import com.cyprusopen.common.refresh.RefreshJob;
import com.cyprusopen.common.refresh.RefreshResult;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/** One conditional request per run; anything but a valid, newer publication keeps the stored directory. */
@Service
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class DirectorySync implements RefreshJob {
    private final ParkingDirectory directory;
    private final DirectoryClient client;
    private final DirectoryXmlParser parser;
    private final Clock clock;

    @Override
    public String name() {
        return "parking-directory";
    }

    @Override
    public boolean needsInitialRun() {
        return directory.snapshot().items().isEmpty();
    }

    @Override
    public RefreshResult refresh() {
        Upstream.Response response;
        try {
            response = client.fetch(directory.validators());
        } catch (RestClientException | IllegalArgumentException unavailable) {
            log.warn("Parking directory source unavailable: {}", unavailable.getMessage());
            return RefreshResult.UNAVAILABLE;
        }
        if (response.status() == HttpStatus.NOT_MODIFIED.value()) {
            if (directory.snapshot().items().isEmpty()) {
                log.warn("Parking directory source answered 304 although no directory is stored");
                return RefreshResult.INVALID;
            }
            directory.markUnchanged(clock.instant());
            return RefreshResult.UNCHANGED;
        }
        if (!response.ok()) {
            log.warn("Parking directory source returned {}", response.status());
            return RefreshResult.UNAVAILABLE;
        }

        ParkingDirectory.Snapshot snapshot;
        try {
            snapshot = parser.parse(response.body(), directory.snapshot(), clock.instant());
        } catch (IllegalArgumentException invalid) {
            log.warn("Parking directory rejected: {}", invalid.getMessage());
            return RefreshResult.INVALID;
        }
        directory.replace(snapshot, response.etag(), response.lastModified());
        return RefreshResult.UPDATED;
    }
}
