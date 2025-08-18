package com.ifride.core.user.service.converter;

import com.ifride.core.auth.model.RegisterRequestDTO;
import com.ifride.core.user.models.Role;
import com.ifride.core.user.models.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserConverter {

    public User from(RegisterRequestDTO registerRequestDTO, Role role) {
        User user = new User();

        user.setEmail(registerRequestDTO.email());
        user.setPassword(new BCryptPasswordEncoder().encode(registerRequestDTO.password()));
        user.setFirstName(registerRequestDTO.firstName());
        user.setLastName(registerRequestDTO.lastName());
        user.setRole(role);

        return user;
    }
}
