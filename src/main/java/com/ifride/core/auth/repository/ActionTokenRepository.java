package com.ifride.core.auth.repository;

import com.ifride.core.auth.model.entity.ActionToken;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActionTokenRepository extends JpaRepository<ActionToken, String> {

    void deleteByUserAndType(User user, TokenType type);
}
