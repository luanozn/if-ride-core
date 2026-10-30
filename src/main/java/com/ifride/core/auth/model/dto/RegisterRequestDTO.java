package com.ifride.core.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegisterRequestDTO(
        @Schema(description = "E-mail institucional ou pessoal", example = "trillian@estudante.ifgoiano.edu.br")
        @NotBlank @Email String email,

        @Schema(description = "Senha (mínimo 8 caracteres)", example = "galaxy123")
        @NotBlank @Size(min = 8, max = 72) String password,

        @Schema(description = "Nome completo", example = "Tricia McMillan")
        @NotBlank String name,

        @Schema(description = "CPF", example = "123.456.789-00")
        @NotBlank String documentNumber,

        @Schema(description = "Data de nascimento", example = "2002-12-02")
        @NotBlank LocalDate birthDate
) {}