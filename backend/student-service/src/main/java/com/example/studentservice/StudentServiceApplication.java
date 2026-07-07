package com.example.studentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Student Service microservice.
 * Responsible for owning the student data domain: registration,
 * profile management, and lookups consumed by other services
 * (e.g. enrollment-service) via REST.
 */
@SpringBootApplication
public class StudentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentServiceApplication.class, args);
    }
}
