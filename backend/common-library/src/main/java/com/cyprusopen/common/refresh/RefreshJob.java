package com.cyprusopen.common.refresh;

/**
 * An upstream import that runs on a schedule. Its timing lives in {@code cyprus-open-refresh.properties}
 * under {@code cyprusopen.refresh.jobs.<name>}; {@link RefreshScheduler} runs it.
 */
public interface RefreshJob {
    /** Key of this job's schedule in {@code cyprus-open-refresh.properties}. */
    String name();

    /** Whether stored data is missing or too old, so the job should also run right after start-up. */
    boolean needsInitialRun();

    /** Fetches, validates and stores the source; failures keep the previous data. */
    RefreshResult refresh();
}
