package com.hospital.booking.event;

import com.hospital.booking.service.EmailService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for appointment events and triggers the e-mail.
 * phase = AFTER_COMMIT means the method runs only once the booking/cancellation is safely saved in the database.
 * If the transaction rolls back (for example the slot was taken), no e-mail is sent, so a patient
 * can never receive a confirmation for an appointment that does not exist.
 */
@Component
public class AppointmentEmailListener {

    private final EmailService emailService;

    public AppointmentEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAppointmentEvent(AppointmentEvent event) {
        emailService.sendAppointmentEmail(event);
    }
}
