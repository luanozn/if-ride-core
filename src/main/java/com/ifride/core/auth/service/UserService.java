package com.ifride.core.auth.service;

import com.ifride.core.auth.model.dto.UserDto;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.model.mapper.UserMapper;
import com.ifride.core.auth.repository.specs.UserSpecs;
import com.ifride.core.shared.exceptions.api.ForbiddenException;
import com.ifride.core.shared.exceptions.api.NotFoundException;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.repository.UserRepository;
import com.ifride.core.shared.model.enums.Status;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final UserMapper mapper;

    public User findById(String id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Usuário %s não encontrado", id));
    }

    public User findByEmail(String email) {
        return repository.findByEmail(email).orElseThrow(() -> new NotFoundException("Usuário %s não encontrado", email));
    }

    public User save(User user) {
        return repository.save(user);
    }

    public User updateUserRole(User user, Role newRole) {
        user.setRole(newRole);

        return save(user);
    }

    @Transactional
    public void deleteAccount(User user) {
        if (user.has(Role.ADMIN)) {
            var activeAdmins = repository.findAllByRoleAndStatusForUpdate(Role.ADMIN, Status.ACTIVE);

            if (activeAdmins.size() <= 1) {
                throw new ForbiddenException("Não é possível deletar o último administrador ativo do sistema. Cadastre outro administrador antes de prosseguir.");
            }
        }
        repository.delete(user);
    }

    public Page<UserDto> findAll(Role role, String search, Pageable pageable, User author) {
        return repository.findAll(UserSpecs.buildSearchSpec(search, role), pageable).map(mapper::toListItem);
    }
}
