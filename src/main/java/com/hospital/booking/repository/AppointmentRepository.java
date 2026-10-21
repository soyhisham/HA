package com.hospital.booking.repository;

import com.hospital.booking.entity.Appointment;
import com.hospital.booking.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Database access for appointments (patient history, doctor schedule, slot availability check). */
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(Long patientId);

    // a doctor's appointments from a date onwards (used for "today" and "upcoming")
    List<Appointment> findByDoctorIdAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAscAppointmentTimeAsc(
            Long doctorId, LocalDate from);

    // a doctor's appointments with a given status on days BEFORE a date (used for "earlier, still not completed")
    List<Appointment> findByDoctorIdAndStatusAndAppointmentDateLessThanOrderByAppointmentDateAscAppointmentTimeAsc(
            Long doctorId, AppointmentStatus status, LocalDate before);

    // true if someone already holds this slot (cancelled rows have slotActive = null, so they are ignored)
    boolean existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndSlotActiveTrue(
            Long doctorId, LocalDate date, LocalTime time);

    // true if the doctor has any appointment at all (history); such doctors cannot be deleted
    boolean existsByDoctorId(Long doctorId);
}
