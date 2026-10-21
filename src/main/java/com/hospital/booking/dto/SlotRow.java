package com.hospital.booking.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** One row of the admin's slot table: the slot plus whether a patient has already booked it. Read-only view object. */
public class SlotRow {

    private final Long id;
    private final LocalDate date;
    private final LocalTime time;
    private final boolean booked;

    public SlotRow(Long id, LocalDate date, LocalTime time, boolean booked) {
        this.id = id;
        this.date = date;
        this.time = time;
        this.booked = booked;
    }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public LocalTime getTime() { return time; }
    public boolean isBooked() { return booked; }
}
