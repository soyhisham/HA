package com.hospital.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the application.
 * @SpringBootApplication turns on auto-configuration and component scanning,
 * so every @Controller, @Service, @Repository and @Configuration under this package is found automatically.
 */
@SpringBootApplication
public class HospitalBookingApplication {
    public static void main(String[] args) {
        SpringApplication.run(HospitalBookingApplication.class, args);
    }
}
