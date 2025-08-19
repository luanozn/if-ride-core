package com.ifride.core.shared.exceptions;

import com.ifride.core.shared.exceptions.api.UnauthorizedException;

public class TokenNotFoundException extends UnauthorizedException {

    public TokenNotFoundException(String message, Object... args) {
        super(message, "TOKEN_NOT_FOUND", args);
    }

}
