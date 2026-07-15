package com.example.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Authentication & Authorization service.
 * Owns user identity: registration, login, JWT issuance/refresh/revocation,
 * and role management (ADMIN / STUDENT). Every other service trusts the
 * JWTs this service issues rather than re-implementing auth.
 */
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
