package com.cyprusopen.parking;

final class DirectoryTestSql {
    static final String RETRY_IS_DEFERRED = """
            SELECT next_attempt_at > current_timestamp + interval '9 days'
            FROM parking_directory WHERE id = 1
            """;
    static final String SYNC_STATUS = "SELECT sync_status FROM parking_directory WHERE id = 1";
    static final String RESET = """
            UPDATE parking_directory
            SET document = NULL, last_checked_at = NULL, etag = NULL, last_modified = NULL,
                consecutive_failures = 0, sync_status = 'not_imported'
            WHERE id = 1
            """;
    static final String ALLOW_NEXT_REQUEST = """
            UPDATE parking_directory SET next_attempt_at = current_timestamp - interval '1 second'
            WHERE id = 1
            """;

    private DirectoryTestSql() {}
}
