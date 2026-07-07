package com.example.enrollmentservice.exception;

public class CourseCapacityExceededException extends RuntimeException {

    public CourseCapacityExceededException(String message) {
        super(message);
    }
}