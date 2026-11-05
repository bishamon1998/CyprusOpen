package com.cyprusopen.statistics;

import java.util.List;

/**
 * One official CYSTAT table served by the statistics API. Implementations own the mapping from the
 * source table's categories to stable series keys; fetching, storage and refresh are shared.
 */
interface StatisticsDataset {
    /** Stable API code, also the primary key in storage. */
    String code();

    /** Table path below the CYSTAT PxWeb API root, for example {@code Population/Migration/1840010E.px}. */
    String table();

    /** data.gov.cy catalog page that links to the table. */
    String catalogUrl();

    /** Maps the table to observations; throws {@link IllegalArgumentException} when the table shape is unexpected. */
    List<Observation> observations(JsonStat table);
}
