package com.ifride.core.config.exceptions;

import com.ifride.core.exceptions.ApiException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class StatusHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApiException.class)
    private ResponseEntity<ErrorResponse> exceptionHandler(ApiException exception) {
        return ResponseEntity.status(exception.getStatus()).body(new ErrorResponse(exception.getMessage(), exception.getStatus().value()));
    }
}
