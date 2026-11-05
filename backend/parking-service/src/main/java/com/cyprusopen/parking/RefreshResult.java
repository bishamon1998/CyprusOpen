package com.cyprusopen.parking;

enum RefreshResult {
    UPDATED,
    UNCHANGED,
    UNAVAILABLE,
    INVALID;

    boolean failed() {
        return this == UNAVAILABLE || this == INVALID;
    }
}
