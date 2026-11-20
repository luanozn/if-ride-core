package com.ifride.core.auth.repository;

import com.ifride.core.auth.model.entity.ActionToken;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.TokenType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActionTokenRepository extends JpaRepository<ActionToken, String> {
    Optional<ActionToken> findByUserAndType(User user, TokenType type);
    void deleteByUserAndType(User user, TokenType type);

    @Modifying
    @Query("DELETE FROM tokens t WHERE t.user.id = :userId")
    void deleteAllByUserId(@Param("userId") String userId);
}
