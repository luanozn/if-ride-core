package com.ifride.core.service;

import com.ifride.core.exceptions.ConflictException;
import com.ifride.core.model.auth.RegisterRequestDTO;
import com.ifride.core.model.auth.User;
import com.ifride.core.repository.UserRepository;
import com.ifride.core.service.converter.UserConverter;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final UserConverter userConverter;

    public User register(RegisterRequestDTO registerRequest) {
        var currentUser = repository.findByEmail(registerRequest.email());

        if(currentUser != null) {
            throw new ConflictException(String.format("Não é possível cadastrar o usuário. O email %s já está cadastrado!", registerRequest.email()));
        }

        return repository.save(userConverter.from(registerRequest));
    }
}
