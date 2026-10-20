package io.github.instaxdutta.winnow.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * The password encoder.
 *
 * <p>BCrypt at cost 12. There is exactly one encoder in this application. A second encoder
 * bean makes the injected encoder ambiguous, and the Spring default cost of 10 is below
 * what a stored password needs.
 */
@Configuration
public class PasswordConfig {

    /** The BCrypt cost factor. */
    public static final int STRENGTH = 12;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(STRENGTH);
    }
}
