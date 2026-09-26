package com.ifride.core.auth.repository;

import com.ifride.core.auth.model.entity.ActionToken;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.TokenType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActionTokenRepository extends JpaRepository<ActionToken, String> {
    Optional<ActionToken> findByUserAndType(User user, TokenType type);
    void deleteByUserAndType(User user, TokenType type);
}
