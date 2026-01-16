package com.ifride.core.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginRequestDTO(
        @Schema(description = "E-mail cadastrado do usuário", example = "luan@ifride.com")
        String email,

        @Schema(description = "Senha em texto plano", example = "teste123")
        String password
) {}
