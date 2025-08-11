package com.ifride.core.exceptions;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {

    public ConflictException(String message, Object... args) {
        super(String.format(message, args), HttpStatus.CONFLICT);
    }
}
