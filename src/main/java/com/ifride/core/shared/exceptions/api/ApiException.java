package com.ifride.core.shared.exceptions.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String details;

    public ApiException(
            @Schema(description = "Mensagem explicativa sobre o erro", example = "Erro interno do servidor!")
            String message,

            @Schema(description = "Código exato do status de erro", example = "4XX || 5XX")
            HttpStatus status,

            @Schema(description = "Detalhes informativos mais específicos sobre o erro (opcional)", example = "4XX || 5XX")
            String details
    ) {
        super(message);
        this.status = status;
        this.details = details;
    }
}
