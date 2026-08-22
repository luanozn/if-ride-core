package com.ifride.core.auth.repository;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {

    User findByEmail(String email);
    boolean existsUserByEmail(String email);
    boolean existsUserByCpf(String cpf);
    Page<User> findAllByRole(Role role, Pageable pageable);
    Page<User> findAllByRoleAndCpf(Role role, String document, Pageable pageable);
}
