package com.hospital.booking.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks the @Valid rules of the registration form without starting Spring. */
class RegisterRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private RegisterRequest validRequest() {
        RegisterRequest r = new RegisterRequest();
        r.setFullName("Pat Ient");
        r.setEmail("pat@example.com");
        r.setPhone("9876543210");
        r.setPassword("Secret123");
        r.setConfirmPassword("Secret123");
        return r;
    }

    private Set<String> invalidFields(RegisterRequest r) {
        return validator.validate(r).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    void validRequest_hasNoViolations() {
        assertThat(invalidFields(validRequest())).isEmpty();
    }

    @Test
    void blankName_isRejected() {
        RegisterRequest r = validRequest();
        r.setFullName("  ");
        assertThat(invalidFields(r)).containsExactly("fullName");
    }

    @Test
    void malformedEmail_isRejected() {
        RegisterRequest r = validRequest();
        r.setEmail("not-an-email");
        assertThat(invalidFields(r)).containsExactly("email");
    }

    @Test
    void badPhone_isRejected() {
        RegisterRequest r = validRequest();
        r.setPhone("12ab");
        assertThat(invalidFields(r)).containsExactly("phone");
    }

    @Test
    void weakPasswords_areRejected() {
        RegisterRequest r = validRequest();
        r.setPassword("short1");            // too short
        assertThat(invalidFields(r)).contains("password");

        r.setPassword("onlyletters");       // no digit
        assertThat(invalidFields(r)).contains("password");

        r.setPassword("12345678");          // no letter
        assertThat(invalidFields(r)).contains("password");
    }
}
