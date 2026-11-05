package com.cyprusopen.parking;

import static com.cyprusopen.parking.DirectoryConstants.INVALID;
import static com.cyprusopen.parking.DirectoryConstants.UNAVAILABLE;
import static com.cyprusopen.parking.DirectoryConstants.UNCHANGED;
import static com.cyprusopen.parking.DirectoryConstants.UPDATED;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class DirectorySync {
    private final ParkingDirectory directory;
    private final DirectoryClient client;
    private final DirectoryXmlParser parser;

    public String refresh() {
        var validators = directory.validators();
        DirectoryClient.Response response;
        try {
            response = client.fetch(validators);
        } catch (RestClientException | IllegalArgumentException unavailable) {
            return UNAVAILABLE;
        }
        if (response.status() == HttpStatus.NOT_MODIFIED.value()) {
            if (directory.snapshot().items().isEmpty()) return INVALID;
            directory.markUnchanged();
            return UNCHANGED;
        }
        if (response.status() != HttpStatus.OK.value()) return UNAVAILABLE;

        var previous = directory.snapshot();
        ParkingDirectory.Snapshot snapshot;
        try {
            snapshot = parser.parse(response.body(), previous, Instant.now());
        } catch (IllegalArgumentException invalid) {
            return INVALID;
        }
        directory.replace(snapshot, response.etag(), response.modified());
        return UPDATED;
    }

}
