package com.hospital.booking.dto;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Form object for adding slots. The admin enters a date, working hours and a slot length,
 * and the service generates all the slots in between (e.g. 09:00 to 12:00 every 30 minutes = 6 slots).
 * @DateTimeFormat tells Spring how to convert the browser's date/time text into LocalDate/LocalTime.
 */
public class TimeSlotForm {

    @NotNull(message = "Date is required")
    @FutureOrPresent(message = "Date cannot be in the past")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate slotDate;

    @NotNull(message = "Start time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime endTime;

    @NotNull(message = "Slot length is required")
    @Min(value = 10, message = "Slot length must be at least 10 minutes")
    @Max(value = 120, message = "Slot length must be at most 120 minutes")
    private Integer intervalMinutes = 30;

    public LocalDate getSlotDate() { return slotDate; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public Integer getIntervalMinutes() { return intervalMinutes; }
    public void setIntervalMinutes(Integer intervalMinutes) { this.intervalMinutes = intervalMinutes; }
}
