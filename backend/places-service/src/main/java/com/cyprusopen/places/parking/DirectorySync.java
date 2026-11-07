package com.cyprusopen.places.parking;

import com.cyprusopen.common.refresh.RefreshJob;
import com.cyprusopen.common.refresh.RefreshResult;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
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
        DirectoryClient.Response response;
        try {
            response = client.fetch(directory.validators());
        } catch (RestClientException | IllegalArgumentException unavailable) {
            return RefreshResult.UNAVAILABLE;
        }
        if (response.status() == HttpStatus.NOT_MODIFIED.value()) {
            if (directory.snapshot().items().isEmpty()) return RefreshResult.INVALID;
            directory.markUnchanged();
            return RefreshResult.UNCHANGED;
        }
        if (response.status() != HttpStatus.OK.value()) return RefreshResult.UNAVAILABLE;

        ParkingDirectory.Snapshot snapshot;
        try {
            snapshot = parser.parse(response.body(), directory.snapshot(), clock.instant());
        } catch (IllegalArgumentException invalid) {
            return RefreshResult.INVALID;
        }
        directory.replace(snapshot, response.etag(), response.modified());
        return RefreshResult.UPDATED;
    }
}
