package com.hospital.booking.service;

import com.hospital.booking.entity.*;
import com.hospital.booking.event.AppointmentEvent;
import com.hospital.booking.exception.BusinessRuleException;
import com.hospital.booking.exception.ResourceNotFoundException;
import com.hospital.booking.repository.AppointmentRepository;
import com.hospital.booking.repository.TimeSlotRepository;
import com.hospital.booking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the booking and cancelling rules. Repositories are mocks, so no database is needed.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppointmentServiceTest {

    private static final String PATIENT_EMAIL = "pat@example.com";
    private static final Long DOCTOR_ID = 10L;
    private static final Long SLOT_ID = 100L;
    private static final Long APPOINTMENT_ID = 500L;

    @Mock AppointmentRepository appointmentRepository;
    @Mock TimeSlotRepository timeSlotRepository;
    @Mock UserRepository userRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks AppointmentService service;

    private User patient;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        patient = new User("Pat Ient", PATIENT_EMAIL, "hash", "9000000001", Role.PATIENT);
        patient.setId(1L);
        User doctorUser = new User("Dr Who", "doc@example.com", "hash", "9000000002", Role.DOCTOR);
        doctorUser.setId(2L);
        doctor = new Doctor(doctorUser, "Cardiology", new BigDecimal("500.00"), 10);
        doctor.setId(DOCTOR_ID);
        when(userRepository.findByEmail(PATIENT_EMAIL)).thenReturn(Optional.of(patient));
    }

    private TimeSlot slotAt(LocalDateTime when) {
        TimeSlot slot = new TimeSlot(doctor, when.toLocalDate(), when.toLocalTime());
        slot.setId(SLOT_ID);
        when(timeSlotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
        return slot;
    }

    private Appointment appointmentAt(LocalDateTime when) {
        Appointment a = new Appointment(patient, doctor, when.toLocalDate(), when.toLocalTime());
        a.setId(APPOINTMENT_ID);
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(a));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        return a;
    }

    // ---------------- booking ----------------

    @Test
    void book_freeFutureSlot_createsBookedAppointmentAndPublishesEvent() {
        slotAt(LocalDateTime.now().plusDays(2));
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndSlotActiveTrue(
                eq(DOCTOR_ID), any(LocalDate.class), any(LocalTime.class))).thenReturn(false);
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment result = service.book(PATIENT_EMAIL, DOCTOR_ID, SLOT_ID);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.BOOKED);
        assertThat(result.getSlotActive()).isTrue();
        assertThat(result.getPatient()).isSameAs(patient);

        ArgumentCaptor<AppointmentEvent> captor = ArgumentCaptor.forClass(AppointmentEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(AppointmentEvent.Type.BOOKED);
        assertThat(captor.getValue().patientEmail()).isEqualTo(PATIENT_EMAIL);
    }

    @Test
    void book_pastSlot_isRejected() {
        slotAt(LocalDateTime.now().minusHours(1));

        assertThatThrownBy(() -> service.book(PATIENT_EMAIL, DOCTOR_ID, SLOT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("past");

        verify(appointmentRepository, never()).saveAndFlush(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void book_slotAlreadyTaken_isRejected() {
        slotAt(LocalDateTime.now().plusDays(2));
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndSlotActiveTrue(
                eq(DOCTOR_ID), any(LocalDate.class), any(LocalTime.class))).thenReturn(true);

        assertThatThrownBy(() -> service.book(PATIENT_EMAIL, DOCTOR_ID, SLOT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("just booked");

        verify(appointmentRepository, never()).saveAndFlush(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void book_whenDatabaseUniqueConstraintFires_showsFriendlyMessageAndSendsNoEvent() {
        slotAt(LocalDateTime.now().plusDays(2));
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndSlotActiveTrue(
                eq(DOCTOR_ID), any(LocalDate.class), any(LocalTime.class))).thenReturn(false);
        // simulates two patients booking at the same instant: the second INSERT violates the unique constraint
        when(appointmentRepository.saveAndFlush(any(Appointment.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate entry"));

        assertThatThrownBy(() -> service.book(PATIENT_EMAIL, DOCTOR_ID, SLOT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("just booked");

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void book_slotOfAnotherDoctor_isNotFound() {
        slotAt(LocalDateTime.now().plusDays(2));

        assertThatThrownBy(() -> service.book(PATIENT_EMAIL, 999L, SLOT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------- cancelling ----------------

    @Test
    void cancel_ownUpcomingAppointment_freesSlotAndPublishesEvent() {
        Appointment appointment = appointmentAt(LocalDateTime.now().plusDays(1));

        service.cancel(PATIENT_EMAIL, APPOINTMENT_ID);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(appointment.getSlotActive()).isNull();   // NULL means the unique constraint no longer blocks the slot

        ArgumentCaptor<AppointmentEvent> captor = ArgumentCaptor.forClass(AppointmentEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(AppointmentEvent.Type.CANCELLED);
    }

    @Test
    void cancel_someoneElsesAppointment_isNotFound() {
        appointmentAt(LocalDateTime.now().plusDays(1));

        assertThatThrownBy(() -> service.cancel("other@example.com", APPOINTMENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void cancel_pastAppointment_isRejected() {
        appointmentAt(LocalDateTime.now().minusHours(2));

        assertThatThrownBy(() -> service.cancel(PATIENT_EMAIL, APPOINTMENT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Past");
    }

    @Test
    void cancel_alreadyCancelledAppointment_isRejected() {
        Appointment appointment = appointmentAt(LocalDateTime.now().plusDays(1));
        appointment.cancel();

        assertThatThrownBy(() -> service.cancel(PATIENT_EMAIL, APPOINTMENT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only booked");
    }
}
