package com.example.enrollmentservice.exception;

/**
 * @deprecated use {@link UpstreamServiceUnavailableException} instead,
 * which covers both student-service and course-service call failures.
 * Kept only so any external code referencing this class name still compiles.
 */
@Deprecated
public class StudentServiceUnavailableException extends UpstreamServiceUnavailableException {
    public StudentServiceUnavailableException(String message) {
        super(message);
    }
}
