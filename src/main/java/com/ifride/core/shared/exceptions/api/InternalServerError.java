package com.ifride.core.shared.exceptions.api;

import org.springframework.http.HttpStatus;

public class InternalServerError extends ApiException {

    public InternalServerError(String message, String details) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, details);
    }
}
