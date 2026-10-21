package com.hospital.booking.dto;

import com.hospital.booking.entity.Appointment;
import com.hospital.booking.entity.Doctor;
import java.time.LocalDate;
import java.util.List;

/** Everything the doctor's dashboard page needs, bundled into one read-only object. */
public class DoctorDashboard {

    private final Doctor doctor;
    private final LocalDate today;
    private final List<Appointment> todayAppointments;     // today, booked or completed
    private final List<Appointment> upcomingAppointments;  // after today, booked
    private final List<Appointment> earlierPending;        // before today but never marked completed

    public DoctorDashboard(Doctor doctor, LocalDate today, List<Appointment> todayAppointments,
                           List<Appointment> upcomingAppointments, List<Appointment> earlierPending) {
        this.doctor = doctor;
        this.today = today;
        this.todayAppointments = todayAppointments;
        this.upcomingAppointments = upcomingAppointments;
        this.earlierPending = earlierPending;
    }

    public Doctor getDoctor() { return doctor; }
    public LocalDate getToday() { return today; }
    public List<Appointment> getTodayAppointments() { return todayAppointments; }
    public List<Appointment> getUpcomingAppointments() { return upcomingAppointments; }
    public List<Appointment> getEarlierPending() { return earlierPending; }
}
