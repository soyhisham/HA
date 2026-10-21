package com.hospital.booking.exception;

/** Thrown when an action breaks a business rule (e.g. deleting a booked slot). The message is safe to show to the user. */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
