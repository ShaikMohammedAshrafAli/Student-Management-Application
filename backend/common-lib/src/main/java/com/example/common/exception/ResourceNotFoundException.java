package com.example.common.exception;

/** Thrown by any service when a requested entity does not exist. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
