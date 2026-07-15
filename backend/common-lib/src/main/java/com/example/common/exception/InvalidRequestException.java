package com.example.common.exception;

/** Thrown for business-rule validation failures that aren't simple field validation. */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
