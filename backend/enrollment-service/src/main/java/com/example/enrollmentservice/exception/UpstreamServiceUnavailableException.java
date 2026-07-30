package com.example.enrollmentservice.exception;

/**
 * Thrown when a call to any upstream dependency (student-service or
 * course-service) fails or times out. Replaces the narrower, older
 * StudentServiceUnavailableException, which is retained only as a
 * deprecated subclass for source compatibility.
 */
public class UpstreamServiceUnavailableException extends RuntimeException {
    public UpstreamServiceUnavailableException(String message) {
        super(message);
    }
}
