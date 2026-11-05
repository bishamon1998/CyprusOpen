package com.cyprusopen.pharmacies;

import java.time.LocalDate;

/** One scheduled duty from the official roster. House telephone numbers are never kept. */
record DutyEntry(LocalDate date, String district, String registrationNumber, String name,
                 String address, String municipality, String phone) {}
