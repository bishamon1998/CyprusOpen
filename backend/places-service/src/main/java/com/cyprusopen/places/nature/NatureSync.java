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
import com.cyprusopen.common.http.Upstream;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
            var picnic = client.file(PICNIC_FILE);
            var camping = client.file(CAMPING_FILE);
            var campingCatalogue = client.file(CAMPING_CATALOGUE_FILE);
            var trailRoutes = client.file(TRAIL_ROUTES_FILE);
            var trailStarts = client.file(TRAIL_STARTS_FILE);
            var trailCatalogue = client.file(TRAIL_CATALOGUE_FILE);
            files = new NatureParser.Files(picnic.body(), camping.body(), campingCatalogue.body(),
                    trailRoutes.body(), trailStarts.body(), trailCatalogue.body(),
                    latest(List.of(picnic, camping, campingCatalogue, trailRoutes, trailStarts, trailCatalogue)));
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

    /** When the source last changed any of the files; null when it sends no Last-Modified. */
    private static Instant latest(List<Upstream.Response> responses) {
        return responses.stream().map(Upstream.Response::lastModifiedAt).filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(null);
    }
}
