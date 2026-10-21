package com.hospital.booking.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The login success handler sends admins and patients to /admin/dashboard and /patient/dashboard.
 * These two simply forward to each role's main screen (doctor management / doctor search).
 * The doctor's dashboard has real content, so it lives in DoctorController.
 */
@Controller
public class DashboardController {

    @GetMapping("/admin/dashboard")
    public String admin() {
        return "redirect:/admin/doctors";
    }

    @GetMapping("/patient/dashboard")
    public String patient() {
        return "redirect:/patient/doctors";
    }
}
