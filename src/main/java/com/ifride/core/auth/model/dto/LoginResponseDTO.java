package com.ifride.core.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponseDTO(
        @Schema(description = "Token JWT para autenticação nas demais rotas")
        String token,

        @Schema(description = "E-mail do usuário logado", example = "trillian@ifride.com")
        String login,

        @Schema(description = "Timestamp de expiração do token")
        Long expireDate
) {}