package com.ifride.core.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record RegisterRequestDTO(
        @Schema(description = "E-mail institucional ou pessoal", example = "trillian@estudante.ifgoiano.edu.br")
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @Schema(description = "Senha (mínimo 8 caracteres)", example = "galaxy123")
        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
        String password,

        @Schema(description = "Nome completo", example = "Tricia McMillan")
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 3, max = 255, message = "O nome deve ter entre 3 e 255 caracteres")
        String name,

        @Schema(description = "CPF (com ou sem formatação)", examples = {"123.456.789-00", "12345678900"})
        @NotBlank(message = "O CPF é obrigatório")
        String documentNumber,

        @Schema(description = "Data de nascimento (deve ser no passado)", example = "2002-12-02")
        @NotNull(message = "A data de nascimento é obrigatória")
        @Past(message = "A data de nascimento deve ser no passado")
        LocalDate birthDate
) {}