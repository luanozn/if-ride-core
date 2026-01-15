package com.ifride.core.shared.exceptions.api;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException{

    public BadRequestException(String message, Object... args) {
        super(String.format(message, args), HttpStatus.BAD_REQUEST, null);
    }
}
