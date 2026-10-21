package com.hospital.booking.config;

import com.hospital.booking.entity.Doctor;
import com.hospital.booking.entity.Role;
import com.hospital.booking.entity.TimeSlot;
import com.hospital.booking.entity.User;
import com.hospital.booking.repository.DoctorRepository;
import com.hospital.booking.repository.TimeSlotRepository;
import com.hospital.booking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Fills an EMPTY database with sample doctors, time slots and one demo patient so the project can be demonstrated immediately.
 * - Runs only when app.seed.enabled=true (set SEED_DEMO_DATA=false to switch it off).
 * - Does nothing if any doctor already exists, so it never touches real data or creates duplicates.
 * - Passwords are BCrypt-hashed like everywhere else; the demo password comes from app.seed.password.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private record DemoDoctor(String name, String email, String phone, String specialty, String fee, int years) { }

    private static final List<DemoDoctor> DOCTORS = List.of(
            new DemoDoctor("Dr. Alice Carter", "dr.carter@hospital.com", "9000000011", "Cardiology", "800.00", 12),
            new DemoDoctor("Dr. Rahul Mehta", "dr.mehta@hospital.com", "9000000012", "Dermatology", "600.00", 8),
            new DemoDoctor("Dr. Sara Khan", "dr.khan@hospital.com", "9000000013", "Pediatrics", "500.00", 6),
            new DemoDoctor("Dr. John Mathew", "dr.mathew@hospital.com", "9000000014", "Orthopedics", "700.00", 15));

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final PasswordEncoder passwordEncoder;
    private final String demoPassword;

    public DemoDataSeeder(UserRepository userRepository, DoctorRepository doctorRepository,
                          TimeSlotRepository timeSlotRepository, PasswordEncoder passwordEncoder,
                          @Value("${app.seed.password}") String demoPassword) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.passwordEncoder = passwordEncoder;
        this.demoPassword = demoPassword;
    }

    @Override
    public void run(String... args) {
        if (doctorRepository.count() > 0) {
            return;   // database already has data: leave it alone
        }

        for (DemoDoctor d : DOCTORS) {
            User user = userRepository.save(new User(d.name(), d.email(),
                    passwordEncoder.encode(demoPassword), d.phone(), Role.DOCTOR));
            Doctor doctor = doctorRepository.save(new Doctor(user, d.specialty(), new BigDecimal(d.fee()), d.years()));
            timeSlotRepository.saveAll(sampleSlots(doctor));
        }

        if (!userRepository.existsByEmail("patient@demo.com")) {
            userRepository.save(new User("Demo Patient", "patient@demo.com",
                    passwordEncoder.encode(demoPassword), "9000000001", Role.PATIENT));
        }
        log.info("Demo data created: {} doctors with slots and the patient patient@demo.com", DOCTORS.size());
    }

    /** 30-minute slots from 09:00 to 17:00 for today (only times still ahead) and the next 5 days. */
    private List<TimeSlot> sampleSlots(Doctor doctor) {
        List<TimeSlot> slots = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        for (int day = 0; day < 6; day++) {
            LocalDate date = today.plusDays(day);
            for (int minutes = 9 * 60; minutes < 17 * 60; minutes += 30) {
                LocalTime time = LocalTime.of(minutes / 60, minutes % 60);
                if (day == 0 && !time.isAfter(now)) continue;   // no slots in the past
                slots.add(new TimeSlot(doctor, date, time));
            }
        }
        return slots;
    }
}
