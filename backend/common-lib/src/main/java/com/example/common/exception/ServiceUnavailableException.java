package com.example.common.exception;

/** Thrown when an inter-service (Feign) call fails or times out. */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}
