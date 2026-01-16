package com.ifride.core.auth.model.dto;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserDto(
        @Schema(description = "ID único do usuário (UUID)", example = "3e58498b-7004-44f2-959c-85f26f2f9f1b")
        String id,

        @Schema(description = "Nome do usuário", example = "Tricia McMillan")
        String name,

        @Schema(description = "E-mail institucional ou pessoal", example = "trillian@ifride.com")
        String email,

        @Schema(description = "Papel/Nível de acesso do usuário no sistema", example = "DRIVER")
        Role role
) {

    public static UserDto fromEntity(User user) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
