package com.ifride.core.shared.exceptions.api;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends ApiException {

    public ForbiddenException(String message, Object... args) {
        super(String.format(message, args), HttpStatus.FORBIDDEN, null);
    }
}
