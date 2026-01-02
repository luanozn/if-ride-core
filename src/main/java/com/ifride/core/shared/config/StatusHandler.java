package com.ifride.core.shared.config;

import com.ifride.core.shared.exceptions.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class StatusHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApiException.class)
    private ResponseEntity<ErrorResponse> exceptionHandler(ApiException exception) {
        return ResponseEntity.status(exception.getStatus()).body(new ErrorResponse(exception.getMessage(), exception.getDetails(), exception.getStatus().value()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    private ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                        "Acesso negado. Você não tem permissão para realizar esta operação.",
                        ex.getMessage(),
                        HttpStatus.FORBIDDEN.value()
                ));
    }

    @ExceptionHandler(AuthenticationException.class)
    private ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(
                        "Não autenticado",
                        "Falha na autenticação: " + ex.getMessage(),
                        HttpStatus.UNAUTHORIZED.value()
                ));
    }

    @ExceptionHandler(Exception.class)
    private ResponseEntity<ErrorResponse> handleGeneralException(Exception exception) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        "Ocorreu um erro interno inesperado.",
                        exception.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value()
                ));
    }
}
