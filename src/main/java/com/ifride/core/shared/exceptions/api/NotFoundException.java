package com.ifride.core.shared.exceptions.api;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {

    public NotFoundException(String message, Object... args) {
        super(String.format(message, args), HttpStatus.NOT_FOUND, null);
    }
}
