package com.cyprusopen.common.refresh;

/** Outcome of synchronizing one upstream source into local storage. */
public enum RefreshResult {
    UPDATED,
    UNCHANGED,
    UNAVAILABLE,
    INVALID;

    public boolean failed() {
        return this == UNAVAILABLE || this == INVALID;
    }
}
