package com.cyprusopen.places.services;

import java.time.Instant;
import java.util.List;

/**
 * State hospitals (Ministry of Health) and Citizen Service Centres (KEP) with their positions.
 *
 * @param retrievedAt          when the files were last downloaded
 * @param hospitalsPublishedAt when the source last changed the hospital file (Last-Modified), null when unknown
 * @param centresPublishedAt   the same for the Citizen Service Centre file
 */
record ServicesSnapshot(Instant retrievedAt, Instant hospitalsPublishedAt, Instant centresPublishedAt,
                        List<Place> places) {
    /**
     * @param kind           "hospital" or "centre"
     * @param name           Greek name, as published (the files have no other language)
     * @param address        Greek address on one line, null when missing
     * @param phone          phone numbers as published ("22603000"), null when missing
     * @param hours          opening hours read from the published text, null when it could not be read
     * @param hoursText      opening hours as published (Greek), null for hospitals
     * @param appointmentUrl where to book a visit, null when not published
     */
    record Place(String id, String kind, String name, String address, String phone, Hours hours, String hoursText,
                 String appointmentUrl, double latitude, double longitude) {}

    /**
     * One weekly opening time.
     *
     * @param days   ISO days of the week, 1 = Monday
     * @param opens  "08:00"
     * @param closes "17:00"
     */
    record Hours(List<Integer> days, String opens, String closes) {}
}
