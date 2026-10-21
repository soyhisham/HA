package com.hospital.booking.service;

import com.hospital.booking.entity.*;
import com.hospital.booking.exception.BusinessRuleException;
import com.hospital.booking.exception.ResourceNotFoundException;
import com.hospital.booking.repository.AppointmentRepository;
import com.hospital.booking.repository.DoctorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Unit tests for the doctor's "mark as completed" rules. No database needed. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DoctorScheduleServiceTest {

    private static final String DOCTOR_EMAIL = "doc@example.com";
    private static final Long APPOINTMENT_ID = 500L;

    @Mock DoctorRepository doctorRepository;
    @Mock AppointmentRepository appointmentRepository;
    @InjectMocks DoctorScheduleService service;

    private User patient;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        patient = new User("Pat Ient", "pat@example.com", "hash", "9000000001", Role.PATIENT);
        patient.setId(1L);
        doctor = newDoctor(10L, DOCTOR_EMAIL);
        when(doctorRepository.findByUserEmail(DOCTOR_EMAIL)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Doctor newDoctor(Long id, String email) {
        User user = new User("Dr " + id, email, "hash", "9000000002", Role.DOCTOR);
        user.setId(id + 1000);
        Doctor d = new Doctor(user, "Cardiology", new BigDecimal("500.00"), 10);
        d.setId(id);
        return d;
    }

    private Appointment appointmentAt(LocalDateTime when, Doctor forDoctor) {
        Appointment a = new Appointment(patient, forDoctor, when.toLocalDate(), when.toLocalTime());
        a.setId(APPOINTMENT_ID);
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(a));
        return a;
    }

    @Test
    void markCompleted_ownAppointmentThatHasStarted_becomesCompleted() {
        Appointment appointment = appointmentAt(LocalDateTime.now().minusHours(1), doctor);

        service.markCompleted(DOCTOR_EMAIL, APPOINTMENT_ID);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(appointment.getSlotActive()).isTrue();   // a completed visit still occupies its slot
    }

    @Test
    void markCompleted_appointmentInTheFuture_isRejected() {
        appointmentAt(LocalDateTime.now().plusHours(3), doctor);

        assertThatThrownBy(() -> service.markCompleted(DOCTOR_EMAIL, APPOINTMENT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not started");
    }

    @Test
    void markCompleted_anotherDoctorsAppointment_isNotFound() {
        appointmentAt(LocalDateTime.now().minusHours(1), newDoctor(11L, "other@example.com"));

        assertThatThrownBy(() -> service.markCompleted(DOCTOR_EMAIL, APPOINTMENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markCompleted_cancelledAppointment_isRejected() {
        Appointment appointment = appointmentAt(LocalDateTime.now().minusHours(1), doctor);
        appointment.cancel();

        assertThatThrownBy(() -> service.markCompleted(DOCTOR_EMAIL, APPOINTMENT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only booked");
    }
}
