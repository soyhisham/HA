package com.hospital.booking.event;

import com.hospital.booking.entity.Appointment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A message saying "an appointment was just booked / cancelled".
 * It is an immutable snapshot of the details needed for the e-mail (plain values, no database entities),
 * so it is safe to hand to another thread after the transaction has finished.
 */
public record AppointmentEvent(
        Type type,
        Long appointmentId,
        String patientName,
        String patientEmail,
        String doctorName,
        String specialty,
        LocalDate date,
        LocalTime time,
        BigDecimal fee) {

    public enum Type { BOOKED, CANCELLED }

    /** Copies the needed details out of the entity. */
    public static AppointmentEvent of(Type type, Appointment a) {
        return new AppointmentEvent(
                type,
                a.getId(),
                a.getPatient().getFullName(),
                a.getPatient().getEmail(),
                a.getDoctor().getName(),
                a.getDoctor().getSpecialty(),
                a.getAppointmentDate(),
                a.getAppointmentTime(),
                a.getDoctor().getFee());
    }
}
