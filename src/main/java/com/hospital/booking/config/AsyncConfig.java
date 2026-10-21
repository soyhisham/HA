package com.hospital.booking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Turns on @Async support. Methods marked @Async (the e-mail sender) run on a background thread,
 * so the patient's page does not wait for the mail server, and a slow or failing mail server cannot break a booking.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
