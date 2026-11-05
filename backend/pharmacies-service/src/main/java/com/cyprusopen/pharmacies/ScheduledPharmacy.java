package com.cyprusopen.pharmacies;

/** A duty entry with its stored map point, if one exists. */
record ScheduledPharmacy(DutyEntry entry, PharmacyLocation location) {}
