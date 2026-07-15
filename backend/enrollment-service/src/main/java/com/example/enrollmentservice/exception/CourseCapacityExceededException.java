package com.example.enrollmentservice.exception;

/** Thrown when trying to enroll a student in a course that is already full. */
public class CourseCapacityExceededException extends RuntimeException {
    public CourseCapacityExceededException(String message) {
        super(message);
    }
}
