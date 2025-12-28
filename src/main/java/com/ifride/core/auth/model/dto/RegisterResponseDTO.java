package com.ifride.core.auth.model.dto;

import com.ifride.core.auth.model.entity.User;

public record RegisterResponseDTO (String id, String email, String name, boolean emailVerified) {

    public static RegisterResponseDTO from(User user) {
        return new RegisterResponseDTO(user.getId(), user.getEmail(), user.getName(), user.isEmailVerified());
    }
}
