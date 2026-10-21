package com.hospital.booking.service;

import com.hospital.booking.dto.DoctorDashboard;
import com.hospital.booking.entity.Appointment;
import com.hospital.booking.entity.AppointmentStatus;
import com.hospital.booking.entity.Doctor;
import com.hospital.booking.exception.BusinessRuleException;
import com.hospital.booking.exception.ResourceNotFoundException;
import com.hospital.booking.repository.AppointmentRepository;
import com.hospital.booking.repository.DoctorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Business logic for the logged-in doctor: see today's and upcoming appointments, and mark visits as completed.
 * The doctor is always identified from the login (email), never from a URL or form field,
 * so one doctor can never see or change another doctor's appointments.
 */
@Service
public class DoctorScheduleService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    public DoctorScheduleService(DoctorRepository doctorRepository, AppointmentRepository appointmentRepository) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
    }

    /** Builds the dashboard: today's list, upcoming list, and older appointments still waiting to be completed. */
    public DoctorDashboard dashboard(String doctorEmail) {
        Doctor doctor = findDoctor(doctorEmail);
        LocalDate today = LocalDate.now();

        // One query for "today and later", then split it in memory; cancelled appointments are left out
        List<Appointment> fromToday = appointmentRepository
                .findByDoctorIdAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAscAppointmentTimeAsc(doctor.getId(), today);

        List<Appointment> todays = fromToday.stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                .filter(a -> a.getAppointmentDate().equals(today))
                .toList();

        List<Appointment> upcoming = fromToday.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.BOOKED)
                .filter(a -> a.getAppointmentDate().isAfter(today))
                .toList();

        List<Appointment> earlier = appointmentRepository
                .findByDoctorIdAndStatusAndAppointmentDateLessThanOrderByAppointmentDateAscAppointmentTimeAsc(
                        doctor.getId(), AppointmentStatus.BOOKED, today);

        return new DoctorDashboard(doctor, today, todays, upcoming, earlier);
    }

    /** BOOKED -> COMPLETED, allowed only for the doctor's own appointment and only once its start time has arrived. */
    @Transactional
    public Appointment markCompleted(String doctorEmail, Long appointmentId) {
        Doctor doctor = findDoctor(doctorEmail);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));

        // Another doctor's appointment: answer "not found" so we do not reveal that it exists
        if (!appointment.getDoctor().getId().equals(doctor.getId())) {
            throw new ResourceNotFoundException("Appointment not found: " + appointmentId);
        }
        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new BusinessRuleException("Only booked appointments can be marked as completed.");
        }
        if (appointment.getStartDateTime().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("This appointment has not started yet.");
        }

        appointment.complete();   // the slot stays occupied (slotActive remains TRUE)
        return appointmentRepository.save(appointment);
    }

    private Doctor findDoctor(String email) {
        return doctorRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No doctor profile for " + email));
    }
}
