package com.hospital.booking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Professional details of a doctor (specialty, fee, experience).
 * Each doctor is linked one-to-one to a User account (role DOCTOR), so the doctor can log in
 * while name/email/password stay in the users table and are not duplicated here.
 */
@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // unique = true enforces "one doctor profile per user account"
    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 100)
    private String specialty;

    // BigDecimal is the correct type for money (no floating-point rounding errors)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fee;

    @Column(name = "experience_years", nullable = false)
    private int experienceYears;

    public Doctor() { }

    public Doctor(User user, String specialty, BigDecimal fee, int experienceYears) {
        this.user = user;
        this.specialty = specialty;
        this.fee = fee;
        this.experienceYears = experienceYears;
    }

    /** Convenience so templates can show doctor.name instead of doctor.user.fullName */
    public String getName() { return user.getFullName(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }
    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }
    public int getExperienceYears() { return experienceYears; }
    public void setExperienceYears(int experienceYears) { this.experienceYears = experienceYears; }
}
