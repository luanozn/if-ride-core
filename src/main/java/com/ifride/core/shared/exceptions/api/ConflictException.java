package com.ifride.core.shared.exceptions.api;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {

    public ConflictException(String message, Object... args) {
        super(String.format(message, args), HttpStatus.CONFLICT, null);
    }
}
