package com.hospital.booking.repository;

import com.hospital.booking.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

/** Database access for doctors, including the specialty search used by patients. */
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findBySpecialtyIgnoreCase(String specialty);  // "search doctors by specialty"
    Optional<Doctor> findByUserEmail(String email);            // finds the logged-in doctor's profile

    @Query("select distinct d.specialty from Doctor d order by d.specialty")
    List<String> findAllSpecialties();                         // fills the specialty dropdown
}
