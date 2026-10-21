package com.hospital.booking.service;

import com.hospital.booking.event.AppointmentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Builds and sends the confirmation / cancellation e-mails using Spring Mail (JavaMailSender).
 * - @Async: runs in the background so the web request is not delayed.
 * - Any mail error is caught and logged, never thrown: the booking is already saved, and a mail problem must not undo it.
 * - If app.mail.enabled=false the e-mail is only written to the log (useful when no SMTP account is set up).
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.enabled}") boolean enabled,
                        @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
    }

    @Async
    public void sendAppointmentEmail(AppointmentEvent event) {
        boolean booked = event.type() == AppointmentEvent.Type.BOOKED;

        String subject = (booked ? "Appointment confirmed: " : "Appointment cancelled: ")
                + event.doctorName() + ", " + event.date().format(DATE_FORMAT);

        String details = """
                Doctor:  %s (%s)
                Date:    %s
                Time:    %s
                Fee:     %s
                """.formatted(
                event.doctorName(), event.specialty(),
                event.date().format(DATE_FORMAT),
                event.time().format(TIME_FORMAT),
                event.fee().setScale(2, RoundingMode.HALF_UP).toPlainString());

        String intro = booked
                ? "Your appointment has been confirmed."
                : "Your appointment has been cancelled.";
        String outro = booked
                ? "If you cannot attend, please cancel it from \"My Appointments\" after logging in."
                : "You can book another appointment any time from the website.";

        String body = "Dear " + event.patientName() + ",\n\n"
                + intro + "\n\n"
                + details + "\n"
                + outro + "\n\n"
                + "Hospital Booking Team";

        send(event.patientEmail(), subject, body);
    }

    private void send(String to, String subject, String body) {
        if (!enabled) {
            log.info("E-mail disabled. Would have sent to {}: {}", to, subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("E-mail sent to {}: {}", to, subject);
        } catch (MailException e) {
            log.error("Could not send e-mail to {}: {}", to, e.getMessage());
        }
    }
}
