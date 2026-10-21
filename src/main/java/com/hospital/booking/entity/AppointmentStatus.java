package com.hospital.booking.entity;

/** Life cycle of an appointment: it starts BOOKED, then becomes either CANCELLED (by patient) or COMPLETED (by doctor). */
public enum AppointmentStatus {
    BOOKED, CANCELLED, COMPLETED
}
