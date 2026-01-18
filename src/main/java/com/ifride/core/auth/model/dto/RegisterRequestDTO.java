package com.ifride.core.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record RegisterRequestDTO(
        @Schema(description = "E-mail institucional ou pessoal", example = "trillian@estudante.ifgoiano.edu.br")
        String email,

        @Schema(description = "Senha (mínimo 8 caracteres)", example = "galaxy123")
        String password,

        @Schema(description = "Nome completo", example = "Tricia McMillan")
        String name,

        @Schema(description = "CPF ou documento acadêmico", example = "123.456.789-00")
        String documentNumber
) {}