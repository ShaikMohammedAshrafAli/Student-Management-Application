package com.example.gradeservice.exception;

/** Thrown when trying to assign a grade against an enrollment that isn't in a gradable state. */
public class InvalidEnrollmentException extends RuntimeException {
    public InvalidEnrollmentException(String message) {
        super(message);
    }
}
