package com.ifride.core.auth.model.dto;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;

public record UserDto(String id, String name, String email, Role role) {

    public static UserDto fromEntity(User user) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
