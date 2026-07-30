package com.example.gradeservice.exception;

/** Thrown when a call to enrollment-service or course-service fails or times out. */
public class UpstreamServiceUnavailableException extends RuntimeException {
    public UpstreamServiceUnavailableException(String message) {
        super(message);
    }
}
