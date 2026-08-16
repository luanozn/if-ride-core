package com.ifride.core.auth.service.converter;

import static com.ifride.core.shared.utils.CpfViewConverter.convertFormatted;

import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.model.entity.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserConverter {

    public User from(RegisterRequestDTO registerRequestDTO, Role role) {
        User user = new User();

        user.setEmail(registerRequestDTO.email());
        user.setPassword(new BCryptPasswordEncoder().encode(registerRequestDTO.password()));
        user.setName(registerRequestDTO.name());
        user.setCpf(convertFormatted(registerRequestDTO.documentNumber()));
        user.setRole(role);

        return user;
    }
}
