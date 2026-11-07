package com.cyprusopen.common.refresh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class RefreshSchedulerTest {
    private static final ZoneId CYPRUS = ZoneId.of("Asia/Nicosia");
    private static final RefreshProperties.Schedule DAILY = new RefreshProperties.Schedule("0 15 6 * * *", false);

    @Test
    void rejectsJobsWithoutAValidScheduleOrWithDuplicateNames() {
        assertThatThrownBy(() -> scheduler(Map.of(), null, job("a", () -> RefreshResult.UPDATED)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("a");
        assertThatThrownBy(() -> scheduler(Map.of("a", new RefreshProperties.Schedule("every day", true)), null,
                job("a", () -> RefreshResult.UPDATED))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> scheduler(Map.of("a", DAILY), null,
                job("a", () -> RefreshResult.UPDATED), job("a", () -> RefreshResult.UPDATED)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Duplicate");
    }

    @Test
    void reportsAnUnexpectedJobFailureAsUnavailable() {
        var failing = job("a", () -> {
            throw new IllegalStateException("bug");
        });
        assertThat(scheduler(Map.of("a", DAILY), null, failing).execute(failing)).isEqualTo(RefreshResult.UNAVAILABLE);
    }

    @Test
    void runOnceRunsTheNamedJobAndFailsTheProcessWhenItFails() {
        var updated = job("a", () -> RefreshResult.UPDATED);
        scheduler(Map.of("a", DAILY), "a", updated).run(new DefaultApplicationArguments());

        var invalid = job("a", () -> RefreshResult.INVALID);
        assertThatThrownBy(() -> scheduler(Map.of("a", DAILY), "a", invalid).run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("INVALID");
        assertThatThrownBy(() -> scheduler(Map.of("a", DAILY), "b", updated).run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Unknown");
    }

    @Test
    void combinesResultsWithFailuresFirstThenUpdates() {
        assertThat(RefreshResult.combine(List.of())).isEqualTo(RefreshResult.UNCHANGED);
        assertThat(RefreshResult.combine(List.of(RefreshResult.UNCHANGED, RefreshResult.UPDATED)))
                .isEqualTo(RefreshResult.UPDATED);
        assertThat(RefreshResult.combine(List.of(RefreshResult.UPDATED, RefreshResult.INVALID,
                RefreshResult.UNAVAILABLE))).isEqualTo(RefreshResult.INVALID);
    }

    private static RefreshScheduler scheduler(Map<String, RefreshProperties.Schedule> schedules, String runOnce,
                                              RefreshJob... jobs) {
        return new RefreshScheduler(List.of(jobs), new RefreshProperties(CYPRUS, schedules, runOnce));
    }

    private static RefreshJob job(String name, Supplier<RefreshResult> refresh) {
        return new RefreshJob() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public boolean needsInitialRun() {
                return false;
            }

            @Override
            public RefreshResult refresh() {
                return refresh.get();
            }
        };
    }
}
