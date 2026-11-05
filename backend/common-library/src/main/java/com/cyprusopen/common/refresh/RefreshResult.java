package com.cyprusopen.common.refresh;

import java.util.Collection;

/** Outcome of synchronizing one upstream source into local storage. */
public enum RefreshResult {
    UPDATED,
    UNCHANGED,
    UNAVAILABLE,
    INVALID;

    public boolean failed() {
        return this == UNAVAILABLE || this == INVALID;
    }

    /** Summarizes several sources: the first failure wins, then any update, otherwise unchanged. */
    public static RefreshResult combine(Collection<RefreshResult> results) {
        return results.stream().filter(RefreshResult::failed).findFirst()
                .orElse(results.contains(UPDATED) ? UPDATED : UNCHANGED);
    }
}
