package com.ifride.core.shared.exceptions.api;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String details;

    public ApiException(String message, HttpStatus status, String details) {
        super(message);
        this.status = status;
        this.details = details;
    }
}
