package com.hospital.booking.controller;

import com.hospital.booking.dto.RegisterRequest;
import com.hospital.booking.exception.EmailAlreadyExistsException;
import com.hospital.booking.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Handles the login page and patient registration.
 * The POST /login itself is handled by Spring Security, not by this controller.
 */
@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                           BindingResult result) {
        // Cross-field rule that annotations on a single field cannot express
        if (request.getPassword() != null && !request.getPassword().equals(request.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
        }
        if (result.hasErrors()) {
            return "register";   // redisplay the form with error messages
        }
        try {
            userService.registerPatient(request);
        } catch (EmailAlreadyExistsException e) {
            result.rejectValue("email", "duplicate", "This email is already registered");
            return "register";
        }
        return "redirect:/login?registered";
    }
}
