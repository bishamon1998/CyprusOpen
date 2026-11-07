package com.cyprusopen.places.parking;

final class DirectoryTestSql {
    static final String RESET = """
            UPDATE parking_directory
            SET document = NULL, last_checked_at = NULL, etag = NULL, last_modified = NULL
            WHERE id = 1
            """;

    private DirectoryTestSql() {}
}
