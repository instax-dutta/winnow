package com.pg.seo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * The password encoder.
 *
 * <p>BCrypt at cost 12. The original shipped two encoder beans, one of which was missing
 * its {@code @Bean} annotation and so never existed, and the live one used the default cost
 * of 10. There is exactly one encoder in this application.
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
