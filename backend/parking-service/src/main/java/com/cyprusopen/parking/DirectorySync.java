package com.cyprusopen.parking;

import static com.cyprusopen.parking.DirectoryConstants.BUSY;
import static com.cyprusopen.parking.DirectoryConstants.DEFERRED;
import static com.cyprusopen.parking.DirectoryConstants.FAILURE_BACKOFF;
import static com.cyprusopen.parking.DirectoryConstants.INVALID;
import static com.cyprusopen.parking.DirectoryConstants.UNAVAILABLE;
import static com.cyprusopen.parking.DirectoryConstants.UNCHANGED;
import static com.cyprusopen.parking.DirectoryConstants.UPDATED;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class DirectorySync {
    private final ParkingDirectory directory;
    private final DirectoryClient client;
    private final DirectoryXmlParser parser;

    @Transactional
    public String refresh() {
        if (!directory.tryLock()) return BUSY;
        if (!directory.isDue()) return DEFERRED;
        directory.markAttempt();

        var validators = directory.validators();
        DirectoryClient.Response response;
        try {
            response = client.fetch(validators);
        } catch (RestClientException | IllegalArgumentException unavailable) {
            return fail(UNAVAILABLE, null);
        }
        if (response.status() == HttpStatus.NOT_MODIFIED.value()) {
            if (directory.snapshot().items().isEmpty()) return fail(INVALID, null);
            directory.markUnchanged();
            return UNCHANGED;
        }
        if (response.status() != HttpStatus.OK.value()) return fail(UNAVAILABLE, response.retryAt());

        var previous = directory.snapshot();
        ParkingDirectory.Snapshot snapshot;
        try {
            snapshot = parser.parse(response.body(), previous, Instant.now());
        } catch (IllegalArgumentException invalid) {
            return fail(INVALID, null);
        }
        directory.replace(snapshot, response.etag(), response.modified());
        return UPDATED;
    }

    private String fail(String result, Instant retryAt) {
        var next = Instant.now().plus(FAILURE_BACKOFF);
        directory.markFailed(retryAt != null && retryAt.isAfter(next) ? retryAt : next);
        return result;
    }
}
