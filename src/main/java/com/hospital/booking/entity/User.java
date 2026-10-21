package com.hospital.booking.entity;

import jakarta.persistence.*;

/**
 * A person who can log in. Patients, doctors and admins are all stored here and told apart by 'role'.
 * The password column will only ever hold a BCrypt hash, never plain text.
 * Table is named "users" because "user" is a reserved word in some databases.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    // Email is the login name, so it must be unique
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    // Stores the BCrypt hash (60 chars); length 100 leaves room
    @Column(nullable = false, length = 100)
    private String password;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING) // stored as text ("PATIENT"), safer than numbers if the enum order changes
    @Column(nullable = false, length = 20)
    private Role role;

    public User() { }

    public User(String fullName, String email, String password, String phone, Role role) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.role = role;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
