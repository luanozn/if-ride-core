package com.ifride.core.service.converter;

import com.ifride.core.model.auth.RegisterRequestDTO;
import com.ifride.core.model.auth.Role;
import com.ifride.core.model.auth.User;
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
