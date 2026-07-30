package com.example.studentservice.config;

import com.example.common.security.JwtTokenValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Kept separate from SecurityConfig on purpose: SecurityConfig depends on
 * JwtAuthenticationFilter (constructor injection), and JwtAuthenticationFilter
 * depends on JwtTokenValidator. If JwtTokenValidator were a @Bean method
 * inside SecurityConfig itself, Spring would need SecurityConfig fully
 * constructed to produce JwtTokenValidator, but SecurityConfig can't be
 * constructed until JwtAuthenticationFilter (and therefore JwtTokenValidator)
 * already exists - a circular dependency. Defining it here breaks the cycle.
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
