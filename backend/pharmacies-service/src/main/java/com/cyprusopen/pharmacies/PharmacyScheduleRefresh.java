package com.cyprusopen.pharmacies;

import static com.cyprusopen.pharmacies.PharmacyConstants.CYPRUS;
import static com.cyprusopen.pharmacies.PharmacyConstants.MAX_SCHEDULE_AGE;

import com.cyprusopen.common.refresh.RefreshJob;
import com.cyprusopen.common.refresh.RefreshResult;
import java.io.IOException;
import java.io.StringReader;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/** Downloads every district schedule and replaces the stored roster only when all of them are valid. */
@Service
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class PharmacyScheduleRefresh implements RefreshJob {
    private final ScheduleClient client;
    private final CatalogDistributions distributions;
    private final PharmacyCsvReader reader;
    private final PharmacySchedules schedules;
    private final Clock clock;

    @Override
    public String name() {
        return "pharmacy-schedule";
    }

    /** The roster is checked again after start-up when the last successful check is over a day old. */
    @Override
    public boolean needsInitialRun() {
        var checkedAt = schedules.checkedAt();
        return checkedAt == null || checkedAt.isBefore(clock.instant().minus(MAX_SCHEDULE_AGE));
    }

    @Override
    public synchronized RefreshResult refresh() {
        Map<District, List<DutyEntry>> downloaded;
        Instant publishedAt;
        try {
            var metadata = client.metadata();
            downloaded = download(metadata);
            publishedAt = distributions.modified(metadata);
        } catch (RestClientException unavailable) {
            log.warn("Pharmacy schedule source unavailable: {}", unavailable.getMessage());
            return RefreshResult.UNAVAILABLE;
        } catch (IOException | IllegalArgumentException invalid) {
            log.warn("Pharmacy schedule rejected: {}", invalid.getMessage());
            return RefreshResult.INVALID;
        }
        schedules.replace(downloaded, clock.instant(), publishedAt);
        return RefreshResult.UPDATED;
    }

    private Map<District, List<DutyEntry>> download(String metadata) throws IOException {
        var today = LocalDate.now(clock.withZone(CYPRUS));
        var result = new EnumMap<District, List<DutyEntry>>(District.class);
        for (var link : distributions.parse(metadata).entrySet()) {
            var entries = reader.read(new StringReader(client.download(link.getValue())), link.getKey());
            if (entries.stream().allMatch(entry -> entry.date().isBefore(today))) {
                throw new IllegalArgumentException("No current or upcoming entries for " + link.getKey().code());
            }
            result.put(link.getKey(), entries);
        }
        return result;
    }
}
