package com.example.enrollmentservice.exception;

public class StudentServiceUnavailableException extends RuntimeException {

    public StudentServiceUnavailableException(String message) {
        super(message);
    }

    public StudentServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}