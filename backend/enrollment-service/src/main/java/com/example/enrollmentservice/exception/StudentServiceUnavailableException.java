package com.example.enrollmentservice.exception;

/** Thrown when enrollment-service cannot reach student-service to validate a student. */
public class StudentServiceUnavailableException extends RuntimeException {
    public StudentServiceUnavailableException(String message) {
        super(message);
    }
}
