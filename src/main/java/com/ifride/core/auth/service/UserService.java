package com.ifride.core.auth.service;

import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.shared.exceptions.api.NotFoundException;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public User updateUserRole(User user, Role newRole) {
        user.setRole(newRole);

        return save(user);
    }

    public void deleteAccount(User user) {
        repository.delete(user);
    }

    public Page<User> findAllByRole(Role role, Pageable pageable) {
        return repository.findAllByRole(role, pageable);
    }

    public Page<User> findAllByRoleAndDocument(Role role, String document, Pageable pageable) {
        return repository.findAllByRoleAndCpf(role, document, pageable);
    }
}
