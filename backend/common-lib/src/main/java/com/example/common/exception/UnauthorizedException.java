package com.example.common.exception;

/** Thrown when a caller is authenticated but not permitted to perform an action. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
