package com.ifride.core.exceptions;

import org.springframework.http.HttpStatus;

public class PreconditionFailedException extends ApiException {

    public PreconditionFailedException(String message, Object... args) {
        super(String.format(message, args), HttpStatus.PRECONDITION_FAILED);
    }
}
