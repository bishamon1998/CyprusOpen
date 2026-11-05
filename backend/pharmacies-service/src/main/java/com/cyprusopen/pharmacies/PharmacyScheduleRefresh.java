package com.cyprusopen.pharmacies;

import static com.cyprusopen.pharmacies.PharmacyConstants.CYPRUS;

import com.cyprusopen.common.refresh.RefreshResult;
import java.io.IOException;
import java.io.StringReader;
import java.time.Clock;
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
class PharmacyScheduleRefresh {
    private final ScheduleClient client;
    private final CatalogDistributions distributions;
    private final PharmacyCsvReader reader;
    private final PharmacySchedules schedules;
    private final Clock clock;

    synchronized RefreshResult refresh() {
        Map<District, List<DutyEntry>> downloaded;
        try {
            downloaded = download();
        } catch (RestClientException unavailable) {
            log.warn("Pharmacy schedule source unavailable: {}", unavailable.getMessage());
            return RefreshResult.UNAVAILABLE;
        } catch (IOException | IllegalArgumentException invalid) {
            log.warn("Pharmacy schedule rejected: {}", invalid.getMessage());
            return RefreshResult.INVALID;
        }
        schedules.replace(downloaded, clock.instant());
        return RefreshResult.UPDATED;
    }

    private Map<District, List<DutyEntry>> download() throws IOException {
        var today = LocalDate.now(clock.withZone(CYPRUS));
        var result = new EnumMap<District, List<DutyEntry>>(District.class);
        for (var link : distributions.parse(client.metadata()).entrySet()) {
            var entries = reader.read(new StringReader(client.download(link.getValue())), link.getKey());
            if (entries.stream().allMatch(entry -> entry.date().isBefore(today))) {
                throw new IllegalArgumentException("No current or upcoming entries for " + link.getKey().code());
            }
            result.put(link.getKey(), entries);
        }
        return result;
    }
}
