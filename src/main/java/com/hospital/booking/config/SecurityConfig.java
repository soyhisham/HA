package com.hospital.booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * Central security configuration:
 *  - which URLs are public and which need a particular role,
 *  - form login (custom /login page, email as the username),
 *  - logout,
 *  - BCrypt password hashing.
 * CSRF protection stays ON (the default); Thymeleaf forms add the token automatically.
 */
@Configuration
public class SecurityConfig {

    /** BCrypt hashes passwords with a random salt; the original password can never be recovered from the hash. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/register", "/error", "/error/**", "/css/**", "/js/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/doctor/**").hasRole("DOCTOR")
                .requestMatchers("/patient/**").hasRole("PATIENT")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")          // the login form field is called "email"
                .successHandler(roleBasedSuccessHandler())
                .failureUrl("/login?error")
                .permitAll())
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll());
        return http.build();
    }

    /** After login, send each role to its own dashboard. */
    private AuthenticationSuccessHandler roleBasedSuccessHandler() {
        return (request, response, authentication) -> {
            String target = "/patient/dashboard";
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                if (authority.getAuthority().equals("ROLE_ADMIN")) { target = "/admin/dashboard"; break; }
                if (authority.getAuthority().equals("ROLE_DOCTOR")) { target = "/doctor/dashboard"; break; }
            }
            response.sendRedirect(request.getContextPath() + target);
        };
    }
}
