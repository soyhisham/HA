package com.hospital.booking.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * A booking made by a patient with a doctor for a date and time.
 *
 * DOUBLE-BOOKING PROTECTION: the database itself has a unique constraint on
 * (doctor_id, appointment_date, appointment_time, slot_active).
 * 'slotActive' is TRUE while the appointment holds the slot (BOOKED/COMPLETED) and NULL once CANCELLED.
 * MySQL treats NULLs as different from each other, so cancelled rows never block the slot,
 * but two active bookings for the same doctor/date/time are impossible, even if two patients click at the same instant.
 */
@Entity
@Table(name = "appointments",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_appt_doctor_date_time_active",
               columnNames = {"doctor_id", "appointment_date", "appointment_time", "slot_active"}))
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "appointment_time", nullable = false)
    private LocalTime appointmentTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status = AppointmentStatus.BOOKED;

    // TRUE while the slot is held, NULL after cancellation (see class comment)
    @Column(name = "slot_active")
    private Boolean slotActive = Boolean.TRUE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Appointment() { }

    public Appointment(User patient, Doctor doctor, LocalDate date, LocalTime time) {
        this.patient = patient;
        this.doctor = doctor;
        this.appointmentDate = date;
        this.appointmentTime = time;
    }

    /** Patient cancels: status changes and the slot is released for other patients. */
    public void cancel() {
        this.status = AppointmentStatus.CANCELLED;
        this.slotActive = null;
    }

    /** Doctor marks the visit as done; the slot stays occupied. */
    public void complete() {
        this.status = AppointmentStatus.COMPLETED;
    }

    /** Date and time combined, handy for "is this in the past?" checks. */
    public LocalDateTime getStartDateTime() {
        return LocalDateTime.of(appointmentDate, appointmentTime);
    }

    /** True if the appointment is still BOOKED and in the future, i.e. the patient may cancel it. */
    public boolean isCancellable() {
        return status == AppointmentStatus.BOOKED && getStartDateTime().isAfter(LocalDateTime.now());
    }

    /** True if the appointment is BOOKED and its start time has arrived, i.e. the doctor may mark it as completed. */
    public boolean isCompletable() {
        return status == AppointmentStatus.BOOKED && !getStartDateTime().isAfter(LocalDateTime.now());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }
    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }
    public LocalTime getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(LocalTime appointmentTime) { this.appointmentTime = appointmentTime; }
    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }
    public Boolean getSlotActive() { return slotActive; }
    public void setSlotActive(Boolean slotActive) { this.slotActive = slotActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
