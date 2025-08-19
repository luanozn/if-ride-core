package com.ifride.core.shared.exceptions;

import com.ifride.core.shared.exceptions.api.ApiException;
import com.ifride.core.shared.exceptions.api.UnauthorizedException;

public class TokenExpiredException extends UnauthorizedException {

    public TokenExpiredException(String message, Object... args) {
        super(message, "TOKEN_EXPIRED", args);
    }

}
