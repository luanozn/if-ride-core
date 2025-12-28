package com.ifride.core.admin.service;

import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.repository.UserRepository;
import com.ifride.core.auth.service.converter.UserConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository repository;
    private final UserConverter userConverter;

    public User registerAdmin(RegisterRequestDTO registerRequest) {
        return this.repository.save(userConverter.from(registerRequest, Role.ADMIN));
    }

    public User registerDriverDirectly(RegisterRequestDTO registerRequest) {
        //TODO: Quando for criada a tabela de driver information, atualizar esse endpoint pra criar o registro lá

        return this.repository.save(userConverter.from(registerRequest, Role.DRIVER));
    }
}
