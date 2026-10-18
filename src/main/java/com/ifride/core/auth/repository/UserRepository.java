package com.ifride.core.auth.repository;

import com.ifride.core.auth.model.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);
    boolean existsUserByEmail(String email);
    boolean existsUserByCpfEncrypted(String encryptedCpf);
}
