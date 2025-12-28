package com.ifride.core.auth.service;

import com.ifride.core.shared.exceptions.api.NotFoundException;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository repository;

    public User findById(String id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Usuário %s não encontrado", id));
    }

    public User save(User user) {
        return repository.save(user);
    }

}
