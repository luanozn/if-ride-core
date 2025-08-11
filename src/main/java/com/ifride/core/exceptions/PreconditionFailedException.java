package com.ifride.core.exceptions;

public class PreconditionFailedException extends RuntimeException {

    public PreconditionFailedException(String message, Object... args) {
        super(String.format(message, args));
    }
}
