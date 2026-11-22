package com.ifride.core.auth.model;

import com.ifride.core.shared.exceptions.api.ApiException;
import org.springframework.http.HttpStatus;

public class TooManyRequestsException extends ApiException {

    public TooManyRequestsException(String message, Object... args) {
        super(String.format(message, args), HttpStatus.TOO_MANY_REQUESTS, null);
    }
}
