package com.hospital.booking.controller;

import com.hospital.booking.exception.BusinessRuleException;
import com.hospital.booking.service.DoctorScheduleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;

/**
 * Doctor screens: today's and upcoming appointments, and marking a visit as completed.
 * Only DOCTOR can reach /doctor/** (rule in SecurityConfig). The doctor is taken from the login (Principal).
 */
@Controller
@RequestMapping("/doctor")
public class DoctorController {

    private final DoctorScheduleService scheduleService;

    public DoctorController(DoctorScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        model.addAttribute("dashboard", scheduleService.dashboard(principal.getName()));
        return "doctor/dashboard";
    }

    @PostMapping("/appointments/{id}/complete")
    public String complete(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            scheduleService.markCompleted(principal.getName(), id);
            redirect.addFlashAttribute("success", "Appointment marked as completed.");
        } catch (BusinessRuleException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/doctor/dashboard";
    }
}
