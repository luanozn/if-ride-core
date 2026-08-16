package com.ifride.core.auth.model.dto;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.shared.utils.CpfViewConverter;
import io.swagger.v3.oas.annotations.media.Schema;

public record RegisterResponseDTO (
        @Schema(description = "ID único do usuário (UUID)")
        String id,

        @Schema(description = "E-mail cadastrado")
        String email,

        @Schema(description = "Nome exibido no perfil")
        String name,

        @Schema(description = "Documento de identificação (CPF)")
        String documentNumber,

        @Schema(description = "Status da verificação de e-mail")
        boolean emailVerified
) {

    public static RegisterResponseDTO from(User user) {
        return new RegisterResponseDTO(user.getId(), user.getEmail(), user.getName(), CpfViewConverter.convert(user.getCpf()), user.isEmailVerified());
    }
}
