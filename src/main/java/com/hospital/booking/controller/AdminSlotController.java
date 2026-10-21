package com.hospital.booking.controller;

import com.hospital.booking.dto.TimeSlotForm;
import com.hospital.booking.exception.BusinessRuleException;
import com.hospital.booking.service.DoctorService;
import com.hospital.booking.service.TimeSlotService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Admin screen to view, add and delete one doctor's available time slots. */
@Controller
@RequestMapping("/admin/doctors/{doctorId}/slots")
public class AdminSlotController {

    private final DoctorService doctorService;
    private final TimeSlotService timeSlotService;

    public AdminSlotController(DoctorService doctorService, TimeSlotService timeSlotService) {
        this.doctorService = doctorService;
        this.timeSlotService = timeSlotService;
    }

    @GetMapping
    public String slots(@PathVariable Long doctorId, Model model) {
        model.addAttribute("slotForm", new TimeSlotForm());
        fillPage(doctorId, model);
        return "admin/slots";
    }

    @PostMapping
    public String addSlots(@PathVariable Long doctorId, @Valid @ModelAttribute("slotForm") TimeSlotForm form,
                           BindingResult result, Model model, RedirectAttributes redirect) {
        // Cross-field rule: end must be after start
        if (form.getStartTime() != null && form.getEndTime() != null
                && !form.getEndTime().isAfter(form.getStartTime())) {
            result.rejectValue("endTime", "order", "End time must be after start time");
        }
        if (result.hasErrors()) {
            fillPage(doctorId, model);
            return "admin/slots";
        }
        try {
            int created = timeSlotService.generate(doctorId, form);
            redirect.addFlashAttribute("success", created + " slot(s) added.");
        } catch (BusinessRuleException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/doctors/" + doctorId + "/slots";
    }

    @PostMapping("/{slotId}/delete")
    public String deleteSlot(@PathVariable Long doctorId, @PathVariable Long slotId, RedirectAttributes redirect) {
        try {
            timeSlotService.delete(doctorId, slotId);
            redirect.addFlashAttribute("success", "Slot deleted.");
        } catch (BusinessRuleException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/doctors/" + doctorId + "/slots";
    }

    // data the page always needs: the doctor and the list of upcoming slots
    private void fillPage(Long doctorId, Model model) {
        model.addAttribute("doctor", doctorService.getById(doctorId));
        model.addAttribute("rows", timeSlotService.upcomingSlots(doctorId));
    }
}
