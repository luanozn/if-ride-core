package com.ifride.core.auth.repository;

import com.ifride.core.auth.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {

    User findByEmail(String email);
    boolean existsUserByEmail(String email);
    boolean existsUserByCpf(String cpf);
}
