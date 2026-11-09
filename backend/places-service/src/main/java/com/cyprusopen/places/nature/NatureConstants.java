package com.cyprusopen.places.nature;

import java.time.Duration;

final class NatureConstants {
    /** The trail routes KML is the largest file (about 1.5 MB). */
    static final int MAX_FILE_BYTES = 4 * 1024 * 1024;
    static final int MAX_SITES = 2000;
    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    static final Duration READ_TIMEOUT = Duration.ofSeconds(30);
    /** The files have not changed since 2023; a start-up refresh runs when the last import is older than this. */
    static final Duration REFRESH_AFTER = Duration.ofDays(30);

    /** Department of Forests files on data.gov.cy, relative to the configured {@code nature.files-url}. */
    static final String PICNIC_FILE = "PicnicSites.kml";
    static final String CAMPING_FILE = "CampingSites.kmz";
    static final String CAMPING_CATALOGUE_FILE = "CampingSitesWithinStateForestsCatalogue_0.csv";
    static final String TRAIL_ROUTES_FILE = "NatureTrailsRoutes.kml";
    static final String TRAIL_STARTS_FILE = "NatureTrailsStartingPoints.kml";
    static final String TRAIL_CATALOGUE_FILE = "NatureTrailsWithinStateForests.csv";

    static final String KML_NAMESPACE = "http://www.opengis.net/kml/2.2";
    /**
     * Greek trail names in the route layer are cut at 50 bytes of UTF-8, so a name of at least this many bytes may
     * be cut. It is completed from the fuller name lists when exactly one matches, otherwise it ends with "…".
     */
    static final int TRAIL_NAME_CUT_BYTES = 47;
    /** Points of one picnic site share a name and lie close together; they are shown as one site. */
    static final double SAME_SITE_METRES = 1500;
    /** Route lines are simplified to this tolerance (about 6 m), enough for a map at street zoom. */
    static final double ROUTE_TOLERANCE_DEGREES = 0.00006;

    static final String SOURCE_NAME = "Department of Forests";
    static final String SOURCE_URL = "https://www.data.gov.cy/el/dataset/245";
    static final String LICENSE = "CC BY 4.0";
    static final String LICENSE_URL = "https://creativecommons.org/licenses/by/4.0/";

    private NatureConstants() {}
}
