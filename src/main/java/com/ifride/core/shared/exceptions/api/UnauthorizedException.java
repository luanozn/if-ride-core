package com.ifride.core.shared.exceptions.api;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message, String details, Object... args) {
        super(String.format(message, args), HttpStatus.UNAUTHORIZED, details);
    }
}
