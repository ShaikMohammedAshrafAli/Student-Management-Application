package com.example.enrollmentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Enrollment Service microservice.
 * Owns courses and the student <-> course enrollment lifecycle.
 * Validates student existence by calling student-service over REST.
 */
@SpringBootApplication
public class EnrollmentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnrollmentServiceApplication.class, args);
    }
}
