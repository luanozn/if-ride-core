package com.ifride.core.user.service;

import com.ifride.core.exceptions.ConflictException;
import com.ifride.core.exceptions.NotFoundException;
import com.ifride.core.exceptions.PreconditionFailedException;
import com.ifride.core.model.auth.RegisterRequestDTO;
import com.ifride.core.user.models.Role;
import com.ifride.core.user.models.User;
import com.ifride.core.user.repository.UserRepository;
import com.ifride.core.user.service.converter.UserConverter;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final UserConverter userConverter;

    public User register(RegisterRequestDTO registerRequest) {
        var currentUser = repository.findByEmail(registerRequest.email());

        if(currentUser != null) {
            throw new ConflictException("Não é possível cadastrar o usuário. O email %s já está cadastrado!", registerRequest.email());
        }

        return repository.save(userConverter.from(registerRequest, Role.PASSENGER));
    }

    public User registerDriver(RegisterRequestDTO registerRequest, User author) {
        var foundUser = repository.findByEmail(registerRequest.email());
        if(foundUser != null) {
            if(authorCanEditUser(author, foundUser)) {
                throw new PreconditionFailedException("User %s is not allowed to turn other users in DRIVER", registerRequest.email());
            }

            if(foundUser.has(Role.DRIVER)) {
                throw new ConflictException("User %s is already taken in DRIVER", registerRequest.email());
            }

            return repository.save(userConverter.from(registerRequest, Role.DRIVER));
        }

        throw new NotFoundException("User %s cannot be converted to DRIVER, because it was not found in database", registerRequest.email());
    }

    public User registerAdmin(RegisterRequestDTO registerRequest) {
        return this.repository.save(userConverter.from(registerRequest, Role.ADMIN));
    }

    private boolean authorCanEditUser(User author, User toBeUpdated) {
        return !Objects.equals(author.getEmail(), toBeUpdated.getEmail()) && !author.has(Role.ADMIN);
    }
}
