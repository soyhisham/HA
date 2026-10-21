package com.hospital.booking.controller;

import com.hospital.booking.dto.DoctorForm;
import com.hospital.booking.entity.Doctor;
import com.hospital.booking.exception.BusinessRuleException;
import com.hospital.booking.exception.EmailAlreadyExistsException;
import com.hospital.booking.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin screens for doctors: list, add, edit, delete.
 * Only ADMIN can reach /admin/** (rule in SecurityConfig).
 * After a successful POST we redirect (Post-Redirect-Get) so refreshing the page does not resubmit the form;
 * the success/error message travels across the redirect as a "flash attribute".
 */
@Controller
@RequestMapping("/admin/doctors")
public class AdminDoctorController {

    private final DoctorService doctorService;

    public AdminDoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("doctors", doctorService.findAll());
        return "admin/doctors";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("doctorForm", new DoctorForm());
        model.addAttribute("editing", false);
        return "admin/doctor-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("doctorForm") DoctorForm form, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        if (form.getPassword() == null || form.getPassword().isBlank()) {
            result.rejectValue("password", "required", "Password is required for a new doctor");
        }
        if (!result.hasErrors()) {
            try {
                Doctor doctor = doctorService.create(form);
                redirect.addFlashAttribute("success", doctor.getName() + " was added. Now add their time slots.");
                return "redirect:/admin/doctors";
            } catch (EmailAlreadyExistsException e) {
                result.rejectValue("email", "duplicate", "This email is already registered");
            }
        }
        model.addAttribute("editing", false);
        return "admin/doctor-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("doctorForm", DoctorForm.from(doctorService.getById(id)));
        model.addAttribute("editing", true);
        model.addAttribute("doctorId", id);
        return "admin/doctor-form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("doctorForm") DoctorForm form,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                Doctor doctor = doctorService.update(id, form);
                redirect.addFlashAttribute("success", doctor.getName() + " was updated.");
                return "redirect:/admin/doctors";
            } catch (EmailAlreadyExistsException e) {
                result.rejectValue("email", "duplicate", "This email is already registered");
            }
        }
        model.addAttribute("editing", true);
        model.addAttribute("doctorId", id);
        return "admin/doctor-form";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            doctorService.delete(id);
            redirect.addFlashAttribute("success", "Doctor deleted.");
        } catch (BusinessRuleException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/doctors";
    }
}
