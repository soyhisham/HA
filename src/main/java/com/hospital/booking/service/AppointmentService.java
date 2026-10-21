package com.hospital.booking.service;

import com.hospital.booking.entity.Appointment;
import com.hospital.booking.entity.AppointmentStatus;
import com.hospital.booking.entity.TimeSlot;
import com.hospital.booking.entity.User;
import com.hospital.booking.event.AppointmentEvent;
import com.hospital.booking.exception.BusinessRuleException;
import com.hospital.booking.exception.ResourceNotFoundException;
import com.hospital.booking.repository.AppointmentRepository;
import com.hospital.booking.repository.TimeSlotRepository;
import com.hospital.booking.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Core business logic of the project: booking and cancelling appointments (patient side).
 * Rules enforced here, on the server, no matter what the browser sends:
 *  1. a patient cannot book a date/time in the past,
 *  2. a doctor's slot can be booked only once (checked first, and guaranteed by the database unique constraint),
 *  3. a patient can only cancel their own, still-upcoming, BOOKED appointments.
 * After a successful booking or cancellation it publishes an AppointmentEvent; the e-mail is sent
 * only after the transaction commits (see AppointmentEmailListener), so this class never talks to the mail server.
 */
@Service
public class AppointmentService {

    private static final String SLOT_TAKEN_MESSAGE =
            "Sorry, that slot was just booked by someone else. Please choose another time.";

    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              TimeSlotRepository timeSlotRepository,
                              UserRepository userRepository,
                              ApplicationEventPublisher eventPublisher) {
        this.appointmentRepository = appointmentRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Books the given slot for the logged-in patient.
     * The browser only sends the slot id; the doctor, date and time are read from the database,
     * so a patient cannot tamper with them.
     */
    @Transactional
    public Appointment book(String patientEmail, Long doctorId, Long slotId) {
        User patient = userRepository.findByEmail(patientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        TimeSlot slot = timeSlotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found: " + slotId));
        if (!slot.getDoctor().getId().equals(doctorId)) {
            throw new ResourceNotFoundException("Slot not found for this doctor");
        }

        // Rule 1: no past dates or times
        if (!LocalDateTime.of(slot.getSlotDate(), slot.getStartTime()).isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("You cannot book a date or time in the past.");
        }

        // Rule 2a: friendly check for the common case
        if (appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndSlotActiveTrue(
                doctorId, slot.getSlotDate(), slot.getStartTime())) {
            throw new BusinessRuleException(SLOT_TAKEN_MESSAGE);
        }

        // Rule 2b: if two patients pass the check above at the same instant, the database unique constraint
        // lets only one INSERT succeed; the loser gets a DataIntegrityViolationException, which we turn into
        // the same friendly message. saveAndFlush forces the INSERT to run here, inside the try block.
        Appointment saved;
        try {
            saved = appointmentRepository.saveAndFlush(
                    new Appointment(patient, slot.getDoctor(), slot.getSlotDate(), slot.getStartTime()));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessRuleException(SLOT_TAKEN_MESSAGE);
        }

        // Announce the booking. The listener sends the e-mail only AFTER this transaction commits.
        eventPublisher.publishEvent(AppointmentEvent.of(AppointmentEvent.Type.BOOKED, saved));
        return saved;
    }

    /** All appointments of the logged-in patient, newest first. */
    public List<Appointment> findForPatient(String patientEmail) {
        User patient = userRepository.findByEmail(patientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        return appointmentRepository.findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(patient.getId());
    }

    /** Cancels a BOOKED, upcoming appointment that belongs to the logged-in patient and frees its slot. */
    @Transactional
    public Appointment cancel(String patientEmail, Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));

        // Someone else's appointment: answer "not found" so we do not reveal that it exists
        if (!appointment.getPatient().getEmail().equals(patientEmail)) {
            throw new ResourceNotFoundException("Appointment not found: " + appointmentId);
        }
        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new BusinessRuleException("Only booked appointments can be cancelled.");
        }
        if (!appointment.getStartDateTime().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Past appointments cannot be cancelled.");
        }

        appointment.cancel();   // status = CANCELLED and slotActive = null, so the slot is free again
        Appointment saved = appointmentRepository.save(appointment);

        eventPublisher.publishEvent(AppointmentEvent.of(AppointmentEvent.Type.CANCELLED, saved));
        return saved;
    }
}
