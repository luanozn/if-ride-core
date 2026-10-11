package com.ifride.core.shared.config;

import com.ifride.core.shared.exceptions.api.ApiException;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Log4j2
@ControllerAdvice
public class StatusHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApiException.class)
    private ResponseEntity<ErrorResponse> exceptionHandler(ApiException exception) {
        return ResponseEntity.status(exception.getStatus()).body(new ErrorResponse(exception.getMessage(), exception.getDetails(), exception.getStatus().value()));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    private ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException exception) {
        log.error(exception);
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        "A operação não pôde ser concluída porque o registro foi modificado por outra requisição.",
                        "Recarregue a página e tente novamente.",
                        HttpStatus.CONFLICT.value()
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    private ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException exception) {
        log.error(exception);
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        "A operação viola uma restrição de integridade dos dados.",
                        "Verifique se o registro já existe ou se os dados estão corretos.",
                        HttpStatus.CONFLICT.value()
                ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    private ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                        "Acesso negado. Você não tem permissão para realizar esta operação.",
                        exception.getMessage(),
                        HttpStatus.FORBIDDEN.value()
                ));
    }

    @ExceptionHandler(AuthenticationException.class)
    private ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(
                        "Não autenticado",
                        "Falha na autenticação: " + exception.getMessage(),
                        HttpStatus.UNAUTHORIZED.value()
                ));
    }

    @ExceptionHandler(Exception.class)
    private ResponseEntity<ErrorResponse> handleGeneralException(Exception exception) {
        log.error("Erro inesperado: ", exception);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        "Ocorreu um erro interno inesperado.",
                        null,
                        HttpStatus.INTERNAL_SERVER_ERROR.value()
                ));
    }
}
