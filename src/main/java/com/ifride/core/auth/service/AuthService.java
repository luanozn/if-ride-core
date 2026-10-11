package com.ifride.core.auth.service;

import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.repository.UserRepository;
import com.ifride.core.auth.service.converter.UserConverter;
import com.ifride.core.auth.utils.UserValidator;
import com.ifride.core.events.models.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final UserConverter userConverter;
    private final UserValidator userValidator;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public User register(RegisterRequestDTO registerRequest) {
        userValidator.validateDocument(registerRequest);

        var saved = repository.save(userConverter.from(registerRequest, Role.PASSENGER));
        eventPublisher.publishEvent(new UserRegisteredEvent(saved));

        return saved;
    }
}
