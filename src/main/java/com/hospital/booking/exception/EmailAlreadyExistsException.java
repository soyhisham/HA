package com.hospital.booking.exception;

/** Thrown when someone registers with an email that is already in use. The controller turns it into a form error. */
public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("Email already registered: " + email);
    }
}
