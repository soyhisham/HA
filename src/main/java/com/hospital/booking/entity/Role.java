package com.hospital.booking.entity;

/** The three kinds of users in the system. Spring Security will turn these into ROLE_PATIENT, ROLE_DOCTOR, ROLE_ADMIN. */
public enum Role {
    PATIENT, DOCTOR, ADMIN
}
