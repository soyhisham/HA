package com.hospital.booking.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Serves the landing page. Controllers only handle web requests and pick the view; business logic lives in services. */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index"; // resolves to templates/index.html
    }
}
