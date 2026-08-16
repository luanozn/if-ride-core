package com.ifride.core.auth.service;

import static com.ifride.core.shared.utils.CpfViewConverter.convertFormatted;

import br.com.caelum.stella.validation.CPFValidator;
import br.com.caelum.stella.validation.InvalidStateException;
import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.repository.UserRepository;
import com.ifride.core.auth.service.converter.UserConverter;
import com.ifride.core.events.models.UserRegisteredEvent;
import com.ifride.core.shared.exceptions.api.BadRequestException;
import com.ifride.core.shared.exceptions.api.ConflictException;
import com.ifride.core.shared.utils.CpfViewConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final UserConverter userConverter;
    private final ApplicationEventPublisher eventPublisher;
    private final CPFValidator cpfValidator = new CPFValidator();

    public User register(RegisterRequestDTO registerRequest) {
        try {
            cpfValidator.assertValid(convertFormatted(registerRequest.documentNumber()));
        } catch (InvalidStateException e) {
            throw new BadRequestException("O CPF inserido é inválido.");
        }

        if (repository.existsUserByEmail(registerRequest.email())) {
            throw new ConflictException("Não é possível cadastrar o usuário. O email %s já está cadastrado!", registerRequest.email());
        } else if (repository.existsUserByCpf(convertFormatted(registerRequest.documentNumber()))) {
            throw new ConflictException("Não é possível cadastrar o usuário. O CPF %s já está cadastrado", CpfViewConverter.convert(registerRequest.documentNumber()));
        }

        var saved = repository.save(userConverter.from(registerRequest, Role.PASSENGER));
        eventPublisher.publishEvent(new UserRegisteredEvent(saved));

        return saved;
    }
}
