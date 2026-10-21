package com.hospital.booking.controller;

import com.hospital.booking.entity.Appointment;
import com.hospital.booking.exception.BusinessRuleException;
import com.hospital.booking.service.AppointmentService;
import com.hospital.booking.service.DoctorService;
import com.hospital.booking.service.TimeSlotService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Patient screens: search doctors by specialty, see a doctor's free slots, book, list and cancel appointments.
 * Only PATIENT can reach /patient/** (rule in SecurityConfig).
 * The logged-in patient's email comes from the Principal (Spring Security), never from a form field.
 */
@Controller
@RequestMapping("/patient")
public class PatientController {

    private final DoctorService doctorService;
    private final TimeSlotService timeSlotService;
    private final AppointmentService appointmentService;

    public PatientController(DoctorService doctorService, TimeSlotService timeSlotService,
                             AppointmentService appointmentService) {
        this.doctorService = doctorService;
        this.timeSlotService = timeSlotService;
        this.appointmentService = appointmentService;
    }

    /** Search page: optional ?specialty=Cardiology filter. */
    @GetMapping("/doctors")
    public String doctors(@RequestParam(required = false) String specialty, Model model) {
        model.addAttribute("doctors", doctorService.search(specialty));
        model.addAttribute("specialties", doctorService.findAllSpecialties());
        model.addAttribute("selectedSpecialty", specialty);
        return "patient/doctors";
    }

    /** One doctor's profile with the slots that can still be booked, grouped by date. */
    @GetMapping("/doctors/{id}")
    public String doctorDetail(@PathVariable Long id, Model model) {
        model.addAttribute("doctor", doctorService.getById(id));
        model.addAttribute("slotsByDate", timeSlotService.availableSlotsByDate(id));
        return "patient/doctor-detail";
    }

    @PostMapping("/doctors/{doctorId}/slots/{slotId}/book")
    public String book(@PathVariable Long doctorId, @PathVariable Long slotId,
                       Principal principal, RedirectAttributes redirect) {
        try {
            Appointment appointment = appointmentService.book(principal.getName(), doctorId, slotId);
            redirect.addFlashAttribute("success", "Appointment booked with " + appointment.getDoctor().getName()
                    + " on " + appointment.getAppointmentDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))
                    + " at " + appointment.getAppointmentTime().format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)) + ".");
            return "redirect:/patient/appointments";
        } catch (BusinessRuleException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/patient/doctors/" + doctorId;   // back to the slot list, which now shows fresh data
        }
    }

    @GetMapping("/appointments")
    public String myAppointments(Principal principal, Model model) {
        model.addAttribute("appointments", appointmentService.findForPatient(principal.getName()));
        return "patient/appointments";
    }

    @PostMapping("/appointments/{id}/cancel")
    public String cancel(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            appointmentService.cancel(principal.getName(), id);
            redirect.addFlashAttribute("success", "Your appointment was cancelled.");
        } catch (BusinessRuleException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/patient/appointments";
    }
}
