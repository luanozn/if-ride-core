package com.ifride.core.auth.repository;

import com.ifride.core.auth.model.entity.User;

import java.util.List;
import java.util.Optional;

import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.shared.model.enums.Status;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);
    boolean existsUserByEmail(String email);
    boolean existsUserByCpfEncrypted(String encryptedCpf);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM users u WHERE u.role = :role AND u.status = :status")
    List<User> findAllByRoleAndStatusForUpdate(@Param("role") Role role, @Param("status") Status status);
}
