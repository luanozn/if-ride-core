package com.ifride.core.auth.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ifride.core.auth.model.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.ToString;

public record UserDto(
        @Schema(description = "ID único do usuário (UUID)", example = "3e58498b-7004-44f2-959c-85f26f2f9f1b")
        String id,

        @Schema(description = "Nome do usuário", example = "Tricia McMillan")
        String name,

        @Schema(description = "E-mail institucional ou pessoal", example = "trillian@ifride.com")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @ToString.Exclude
        String email,

        @Schema(description = "Cadastro de pessoa física", example = "123.456.789-00")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @ToString.Exclude
        String cpf,

        @Schema(description = "Papel/Nível de acesso do usuário no sistema", example = "DRIVER")
        Role role,

        @Schema(description = "Data de nascimento do usuário", example = "1990-01-25")
        LocalDate birthDate,

        @Schema(description = "Flag que valida se o usuário verificou o email ou não.", example = "true")
        boolean emailVerified
) { }
