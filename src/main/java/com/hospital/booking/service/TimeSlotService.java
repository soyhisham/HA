package com.hospital.booking.service;

import com.hospital.booking.dto.SlotRow;
import com.hospital.booking.dto.TimeSlotForm;
import com.hospital.booking.entity.Appointment;
import com.hospital.booking.entity.Doctor;
import com.hospital.booking.entity.TimeSlot;
import com.hospital.booking.exception.BusinessRuleException;
import com.hospital.booking.exception.ResourceNotFoundException;
import com.hospital.booking.repository.AppointmentRepository;
import com.hospital.booking.repository.TimeSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business logic for a doctor's available time slots (used by the admin).
 * A slot is a time a patient MAY book; whether it is already booked is worked out from the appointments table.
 */
@Service
public class TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorService doctorService;

    public TimeSlotService(TimeSlotRepository timeSlotRepository, AppointmentRepository appointmentRepository,
                           DoctorService doctorService) {
        this.timeSlotRepository = timeSlotRepository;
        this.appointmentRepository = appointmentRepository;
        this.doctorService = doctorService;
    }

    /** Upcoming slots of a doctor, each marked booked/available. Uses two queries in total, not one per slot. */
    public List<SlotRow> upcomingSlots(Long doctorId) {
        LocalDate today = LocalDate.now();

        Set<LocalDateTime> booked = appointmentRepository
                .findByDoctorIdAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAscAppointmentTimeAsc(doctorId, today)
                .stream()
                .filter(a -> Boolean.TRUE.equals(a.getSlotActive()))   // cancelled appointments don't hold the slot
                .map(a -> LocalDateTime.of(a.getAppointmentDate(), a.getAppointmentTime()))
                .collect(Collectors.toSet());

        return timeSlotRepository
                .findByDoctorIdAndSlotDateGreaterThanEqualOrderBySlotDateAscStartTimeAsc(doctorId, today)
                .stream()
                .map(s -> new SlotRow(s.getId(), s.getSlotDate(), s.getStartTime(),
                        booked.contains(LocalDateTime.of(s.getSlotDate(), s.getStartTime()))))
                .toList();
    }

    /** Slots a patient can still book: not already booked and not in the past. */
    public List<SlotRow> availableSlots(Long doctorId) {
        LocalDateTime now = LocalDateTime.now();
        return upcomingSlots(doctorId).stream()
                .filter(row -> !row.isBooked())
                .filter(row -> LocalDateTime.of(row.getDate(), row.getTime()).isAfter(now))
                .toList();
    }

    /** Same as availableSlots but grouped by date (in date order) for display. */
    public Map<LocalDate, List<SlotRow>> availableSlotsByDate(Long doctorId) {
        return availableSlots(doctorId).stream()
                .collect(Collectors.groupingBy(SlotRow::getDate, LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * Creates every slot between start and end on the given date, one every 'intervalMinutes'.
     * Slots that already exist (or are already in the past today) are skipped.
     * @return how many new slots were created
     */
    @Transactional
    public int generate(Long doctorId, TimeSlotForm form) {
        Doctor doctor = doctorService.getById(doctorId);
        LocalDate date = form.getSlotDate();

        Set<LocalTime> existing = timeSlotRepository
                .findByDoctorIdAndSlotDateGreaterThanEqualOrderBySlotDateAscStartTimeAsc(doctorId, date)
                .stream()
                .filter(s -> s.getSlotDate().equals(date))
                .map(TimeSlot::getStartTime)
                .collect(Collectors.toSet());

        int startMinutes = form.getStartTime().getHour() * 60 + form.getStartTime().getMinute();
        int endMinutes = form.getEndTime().getHour() * 60 + form.getEndTime().getMinute();
        int step = form.getIntervalMinutes();
        boolean isToday = date.equals(LocalDate.now());
        LocalTime now = LocalTime.now();

        List<TimeSlot> toSave = new ArrayList<>();
        for (int m = startMinutes; m + step <= endMinutes; m += step) {   // a slot must finish before the end time
            LocalTime time = LocalTime.of(m / 60, m % 60);
            if (isToday && time.isBefore(now)) continue;   // no slots in the past
            if (existing.contains(time)) continue;         // no duplicates
            toSave.add(new TimeSlot(doctor, date, time));
        }

        if (toSave.isEmpty()) {
            throw new BusinessRuleException("No new slots were created. They may already exist, or the time range is too short.");
        }
        timeSlotRepository.saveAll(toSave);
        return toSave.size();
    }

    /** Deletes a slot, unless a patient has booked it. The doctorId in the URL must match the slot's doctor. */
    @Transactional
    public void delete(Long doctorId, Long slotId) {
        TimeSlot slot = timeSlotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found: " + slotId));
        if (!slot.getDoctor().getId().equals(doctorId)) {
            throw new ResourceNotFoundException("Slot not found for this doctor");
        }
        if (appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndSlotActiveTrue(
                doctorId, slot.getSlotDate(), slot.getStartTime())) {
            throw new BusinessRuleException("This slot is booked by a patient and cannot be deleted.");
        }
        timeSlotRepository.delete(slot);
    }
}
