package com.example.enrollmentservice.config;

import com.example.common.security.JwtTokenValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Kept separate from SecurityConfig on purpose - see the equivalent
 * class in student-service for the full explanation. In short:
 * SecurityConfig depends on JwtAuthenticationFilter, which depends on
 * JwtTokenValidator; defining JwtTokenValidator as a @Bean inside
 * SecurityConfig itself would create a circular dependency.
 */
@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public JwtTokenValidator jwtTokenValidator() {
        return new JwtTokenValidator(jwtSecret);
    }
}
