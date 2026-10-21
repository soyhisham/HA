package com.hospital.booking.service;

import com.hospital.booking.dto.RegisterRequest;
import com.hospital.booking.entity.Role;
import com.hospital.booking.entity.User;
import com.hospital.booking.exception.EmailAlreadyExistsException;
import com.hospital.booking.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Business logic for user accounts. Self-registration always creates a PATIENT;
 * doctors and admins are created by an admin (Phase 3) or at startup (DataInitializer).
 * The password is hashed with BCrypt here, before it ever reaches the database.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerPatient(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // Friendly check first...
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        User user = new User(
                request.getFullName().trim(),
                email,
                passwordEncoder.encode(request.getPassword()),   // BCrypt hash, never plain text
                request.getPhone().trim(),
                Role.PATIENT);

        // ...and the database's unique constraint as the safety net if two people register at the same moment
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyExistsException(email);
        }
    }
}
