package com.hospital.booking.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A time the admin has made available for a doctor (e.g. Dr. Rao, 2025-01-10, 10:30).
 * A slot is "free" when no active appointment exists for the same doctor/date/time.
 * The unique constraint stops the admin creating the same slot twice.
 */
@Entity
@Table(name = "time_slots",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_slot_doctor_date_time",
               columnNames = {"doctor_id", "slot_date", "start_time"}))
public class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    public TimeSlot() { }

    public TimeSlot(Doctor doctor, LocalDate slotDate, LocalTime startTime) {
        this.doctor = doctor;
        this.slotDate = slotDate;
        this.startTime = startTime;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }
    public LocalDate getSlotDate() { return slotDate; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
}
