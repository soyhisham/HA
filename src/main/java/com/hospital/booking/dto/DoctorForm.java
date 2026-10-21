package com.hospital.booking.dto;

import com.hospital.booking.entity.Doctor;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Form object used by the admin to add or edit a doctor.
 * It combines account fields (name, email, phone, password) and professional fields (specialty, fee, experience),
 * because one form creates both a User (role DOCTOR) and a Doctor row.
 * The password pattern also accepts an empty value so that, when editing, a blank password means "keep the current one".
 */
public class DoctorForm {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must be at most 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 150, message = "Email must be at most 150 characters")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,13}$", message = "Enter a valid phone number (10 to 13 digits)")
    private String phone;

    @Pattern(regexp = "^$|^(?=.*[A-Za-z])(?=.*\\d).{8,64}$",
             message = "Password must be 8 to 64 characters and contain a letter and a number")
    private String password;

    @NotBlank(message = "Specialty is required")
    @Size(max = 100, message = "Specialty must be at most 100 characters")
    private String specialty;

    @NotNull(message = "Fee is required")
    @DecimalMin(value = "0.0", message = "Fee cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Fee can have at most 2 decimal places")
    private BigDecimal fee;

    @NotNull(message = "Experience is required")
    @Min(value = 0, message = "Experience cannot be negative")
    @Max(value = 60, message = "Experience cannot be more than 60 years")
    private Integer experienceYears;

    /** Pre-fills the form when editing an existing doctor (password stays blank). */
    public static DoctorForm from(Doctor doctor) {
        DoctorForm form = new DoctorForm();
        form.fullName = doctor.getUser().getFullName();
        form.email = doctor.getUser().getEmail();
        form.phone = doctor.getUser().getPhone();
        form.specialty = doctor.getSpecialty();
        form.fee = doctor.getFee();
        form.experienceYears = doctor.getExperienceYears();
        return form;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }
    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }
    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }
}
