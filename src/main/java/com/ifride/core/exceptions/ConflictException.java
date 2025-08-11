package com.ifride.core.exceptions;

public class ConflictException extends RuntimeException {

    public ConflictException(String message, Object... args) {
        super(String.format(message, args));
    }
}
