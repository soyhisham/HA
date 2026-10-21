package com.hospital.booking.service;

import com.hospital.booking.dto.TimeSlotForm;
import com.hospital.booking.entity.*;
import com.hospital.booking.exception.BusinessRuleException;
import com.hospital.booking.repository.AppointmentRepository;
import com.hospital.booking.repository.TimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Unit tests for slot generation and slot deletion. No database needed. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TimeSlotServiceTest {

    private static final Long DOCTOR_ID = 10L;

    @Mock TimeSlotRepository timeSlotRepository;
    @Mock AppointmentRepository appointmentRepository;
    @Mock DoctorService doctorService;
    @InjectMocks TimeSlotService service;
    @Captor ArgumentCaptor<List<TimeSlot>> savedSlots;

    private Doctor doctor;
    private final LocalDate date = LocalDate.now().plusDays(3);

    @BeforeEach
    void setUp() {
        User user = new User("Dr Who", "doc@example.com", "hash", "9000000002", Role.DOCTOR);
        doctor = new Doctor(user, "Cardiology", new BigDecimal("500.00"), 10);
        doctor.setId(DOCTOR_ID);
        when(doctorService.getById(DOCTOR_ID)).thenReturn(doctor);
    }

    private TimeSlotForm form(String from, String to, int minutes) {
        TimeSlotForm f = new TimeSlotForm();
        f.setSlotDate(date);
        f.setStartTime(LocalTime.parse(from));
        f.setEndTime(LocalTime.parse(to));
        f.setIntervalMinutes(minutes);
        return f;
    }

    @Test
    void generate_createsOneSlotPerInterval() {
        when(timeSlotRepository.findByDoctorIdAndSlotDateGreaterThanEqualOrderBySlotDateAscStartTimeAsc(DOCTOR_ID, date))
                .thenReturn(List.of());

        int created = service.generate(DOCTOR_ID, form("09:00", "11:00", 30));

        assertThat(created).isEqualTo(4);
        verify(timeSlotRepository).saveAll(savedSlots.capture());
        assertThat(savedSlots.getValue()).extracting(TimeSlot::getStartTime)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0), LocalTime.of(10, 30));
    }

    @Test
    void generate_skipsSlotsThatAlreadyExist() {
        TimeSlot existing = new TimeSlot(doctor, date, LocalTime.of(9, 30));
        when(timeSlotRepository.findByDoctorIdAndSlotDateGreaterThanEqualOrderBySlotDateAscStartTimeAsc(DOCTOR_ID, date))
                .thenReturn(List.of(existing));

        int created = service.generate(DOCTOR_ID, form("09:00", "11:00", 30));

        assertThat(created).isEqualTo(3);
        verify(timeSlotRepository).saveAll(savedSlots.capture());
        assertThat(savedSlots.getValue()).extracting(TimeSlot::getStartTime)
                .doesNotContain(LocalTime.of(9, 30));
    }

    @Test
    void generate_rangeShorterThanOneSlot_isRejected() {
        when(timeSlotRepository.findByDoctorIdAndSlotDateGreaterThanEqualOrderBySlotDateAscStartTimeAsc(DOCTOR_ID, date))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.generate(DOCTOR_ID, form("09:00", "09:20", 30)))
                .isInstanceOf(BusinessRuleException.class);

        verify(timeSlotRepository, never()).saveAll(any());
    }

    @Test
    void delete_bookedSlot_isRejected() {
        TimeSlot slot = new TimeSlot(doctor, date, LocalTime.of(9, 0));
        slot.setId(100L);
        when(timeSlotRepository.findById(100L)).thenReturn(Optional.of(slot));
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndSlotActiveTrue(
                eq(DOCTOR_ID), eq(date), eq(LocalTime.of(9, 0)))).thenReturn(true);

        assertThatThrownBy(() -> service.delete(DOCTOR_ID, 100L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("booked");

        verify(timeSlotRepository, never()).delete(any(TimeSlot.class));
    }

    @Test
    void delete_freeSlot_isDeleted() {
        TimeSlot slot = new TimeSlot(doctor, date, LocalTime.of(9, 0));
        slot.setId(100L);
        when(timeSlotRepository.findById(100L)).thenReturn(Optional.of(slot));
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndSlotActiveTrue(
                eq(DOCTOR_ID), eq(date), eq(LocalTime.of(9, 0)))).thenReturn(false);

        service.delete(DOCTOR_ID, 100L);

        verify(timeSlotRepository).delete(slot);
    }
}
