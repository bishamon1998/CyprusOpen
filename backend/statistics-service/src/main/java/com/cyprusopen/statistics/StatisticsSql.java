package com.cyprusopen.statistics;

final class StatisticsSql {
    static final String DATASETS = "SELECT code, source_updated_at, checked_at FROM dataset";
    static final String DATASET = "SELECT code, source_updated_at, checked_at FROM dataset WHERE code = :code";
    static final String UPSERT_DATASET = """
            INSERT INTO dataset (code, source_updated_at, checked_at)
            VALUES (:code, :updated, :checked)
            ON CONFLICT (code) DO UPDATE
            SET source_updated_at = excluded.source_updated_at, checked_at = excluded.checked_at
            """;
    static final String MARK_CHECKED = "UPDATE dataset SET checked_at = :checked WHERE code = :code";
    static final String DELETE_OBSERVATIONS = "DELETE FROM observation WHERE dataset_code = :code";
    static final String INSERT_OBSERVATION = """
            INSERT INTO observation (dataset_code, series, period, value, low_reliability)
            VALUES (?, ?, ?, ?, ?)
            """;
    static final String OBSERVATIONS = """
            SELECT series, period, value, low_reliability
            FROM observation WHERE dataset_code = :code
            ORDER BY series, period
            """;

    private StatisticsSql() {}
}
