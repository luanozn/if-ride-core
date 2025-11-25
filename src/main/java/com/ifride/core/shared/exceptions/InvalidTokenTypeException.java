package com.ifride.core.shared.exceptions;

import com.ifride.core.shared.exceptions.api.UnauthorizedException;

public class InvalidTokenTypeException extends UnauthorizedException {

    public InvalidTokenTypeException(String message, Object... args) {
        super(message, "INVALID_TOKEN_TYPE", args);
    }
}
