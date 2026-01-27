package com.ifride.core.auth.service;

import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.repository.UserRepository;
import com.ifride.core.auth.service.converter.UserConverter;
import com.ifride.core.events.models.UserRegisteredEvent;
import com.ifride.core.shared.exceptions.api.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final UserConverter userConverter;
    private final ApplicationEventPublisher eventPublisher;

    public User register(RegisterRequestDTO registerRequest) {
        var currentUser = repository.findByEmail(registerRequest.email());

        if(currentUser != null) {
            throw new ConflictException("Não é possível cadastrar o usuário. O email %s já está cadastrado!", registerRequest.email());
        }

        var saved = repository.save(userConverter.from(registerRequest, Role.PASSENGER));
        eventPublisher.publishEvent(new UserRegisteredEvent(saved));

        return saved;
    }
}
