package com.cyprusopen.parking;

final class DirectorySql {
    static final String READ = """
            SELECT document::text, last_checked_at
            FROM parking_directory WHERE id = :id
            """;
    static final String REPLACE = """
            UPDATE parking_directory
            SET document = CAST(:document AS jsonb), last_checked_at = :checked,
                etag = :etag, last_modified = :modified
            WHERE id = :id
            """;
    static final String VALIDATORS = """
            SELECT etag, last_modified FROM parking_directory
            WHERE id = :id AND document IS NOT NULL
            """;
    static final String MARK_UNCHANGED = """
            UPDATE parking_directory
            SET last_checked_at = :checked
            WHERE id = :id
            """;

    private DirectorySql() {}
}
