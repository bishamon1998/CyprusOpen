package com.cyprusopen.parking;

final class DirectorySql {
    static final String READ = """
            SELECT document::text, last_checked_at
            FROM parking_directory WHERE id = :id
            """;
    static final String REPLACE = """
            UPDATE parking_directory
            SET document = CAST(:document AS jsonb), last_checked_at = :checked,
                etag = :etag, last_modified = :modified,
                consecutive_failures = 0, sync_status = :status
            WHERE id = :id
            """;
    static final String TRY_LOCK = "SELECT pg_try_advisory_xact_lock(:lockId)";
    static final String IS_DUE = """
            SELECT next_attempt_at <= current_timestamp AND consecutive_failures < :maxFailures
            FROM parking_directory WHERE id = :id
            """;
    static final String MARK_ATTEMPT = """
            UPDATE parking_directory
            SET last_attempt_at = current_timestamp,
                next_attempt_at = current_timestamp + :spacing * interval '1 second'
            WHERE id = :id
            """;
    static final String VALIDATORS = "SELECT etag, last_modified FROM parking_directory WHERE id = :id";
    static final String MARK_UNCHANGED = """
            UPDATE parking_directory
            SET last_checked_at = current_timestamp, consecutive_failures = 0, sync_status = :status
            WHERE id = :id
            """;
    static final String MARK_FAILED = """
            UPDATE parking_directory
            SET consecutive_failures = consecutive_failures + 1,
                sync_status = CASE WHEN consecutive_failures + 1 >= :maxFailures THEN :paused ELSE :error END,
                next_attempt_at = :next
            WHERE id = :id
            """;

    private DirectorySql() {}
}
