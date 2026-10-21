package com.hospital.booking.repository;

import com.hospital.booking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/** Database access for users. Spring Data generates the SQL from the method names. */
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);   // used by login
    boolean existsByEmail(String email);        // used by registration to reject duplicates
}
