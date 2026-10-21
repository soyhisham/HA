package com.hospital.booking.service;

import com.hospital.booking.dto.DoctorForm;
import com.hospital.booking.entity.Doctor;
import com.hospital.booking.entity.Role;
import com.hospital.booking.entity.User;
import com.hospital.booking.exception.BusinessRuleException;
import com.hospital.booking.exception.EmailAlreadyExistsException;
import com.hospital.booking.exception.ResourceNotFoundException;
import com.hospital.booking.repository.AppointmentRepository;
import com.hospital.booking.repository.DoctorRepository;
import com.hospital.booking.repository.TimeSlotRepository;
import com.hospital.booking.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
import java.util.List;

/**
 * Business logic for managing doctors (used by the admin).
 * Adding a doctor creates TWO rows (a User with role DOCTOR and a Doctor profile) inside one transaction,
 * so either both are saved or neither is.
 */
@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final AppointmentRepository appointmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DoctorService(DoctorRepository doctorRepository, UserRepository userRepository,
                         TimeSlotRepository timeSlotRepository, AppointmentRepository appointmentRepository,
                         PasswordEncoder passwordEncoder) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.appointmentRepository = appointmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Doctor> findAll() {
        return doctorRepository.findAll(Sort.by("user.fullName"));
    }

    public Doctor getById(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + id));
    }

    /** Patient search: doctors of one specialty (case-insensitive), or all doctors when no specialty is chosen. */
    public List<Doctor> search(String specialty) {
        if (specialty == null || specialty.isBlank()) {
            return findAll();
        }
        return doctorRepository.findBySpecialtyIgnoreCase(specialty.trim()).stream()
                .sorted(Comparator.comparing(Doctor::getName))
                .toList();
    }

    /** Distinct specialties for the search dropdown. */
    public List<String> findAllSpecialties() {
        return doctorRepository.findAllSpecialties();
    }

    @Transactional
    public Doctor create(DoctorForm form) {
        if (form.getPassword() == null || form.getPassword().isBlank()) {
            throw new BusinessRuleException("Password is required for a new doctor");
        }
        String email = form.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        try {
            User user = userRepository.saveAndFlush(new User(
                    form.getFullName().trim(), email,
                    passwordEncoder.encode(form.getPassword()),   // BCrypt hash
                    form.getPhone().trim(), Role.DOCTOR));
            return doctorRepository.save(new Doctor(user, form.getSpecialty().trim(),
                    form.getFee(), form.getExperienceYears()));
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyExistsException(email);   // unique-email constraint hit by a simultaneous request
        }
    }

    @Transactional
    public Doctor update(Long id, DoctorForm form) {
        Doctor doctor = getById(id);
        User user = doctor.getUser();
        String email = form.getEmail().trim().toLowerCase();

        // Only complain about a duplicate if the email actually changed
        if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        user.setFullName(form.getFullName().trim());
        user.setEmail(email);
        user.setPhone(form.getPhone().trim());
        if (form.getPassword() != null && !form.getPassword().isBlank()) {   // blank = keep current password
            user.setPassword(passwordEncoder.encode(form.getPassword()));
        }
        doctor.setSpecialty(form.getSpecialty().trim());
        doctor.setFee(form.getFee());
        doctor.setExperienceYears(form.getExperienceYears());

        try {
            userRepository.saveAndFlush(user);
            return doctorRepository.saveAndFlush(doctor);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyExistsException(email);
        }
    }

    /**
     * Deletes a doctor together with their time slots and login account.
     * A doctor who already has appointments is kept, so patients' appointment history is never lost.
     */
    @Transactional
    public void delete(Long id) {
        Doctor doctor = getById(id);
        if (appointmentRepository.existsByDoctorId(id)) {
            throw new BusinessRuleException(doctor.getName()
                    + " has appointment history and cannot be deleted.");
        }
        timeSlotRepository.deleteByDoctorId(id);
        doctorRepository.delete(doctor);
        userRepository.delete(doctor.getUser());
    }
}
