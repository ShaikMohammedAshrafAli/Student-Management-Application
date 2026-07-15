package com.example.common.exception;

/** Thrown by any service when a uniqueness constraint would be violated. */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
