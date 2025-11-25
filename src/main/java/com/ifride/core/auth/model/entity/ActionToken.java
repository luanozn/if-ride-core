package com.ifride.core.auth.model.entity;

import com.ifride.core.auth.model.enums.TokenType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Table(name = "tokens")
@Entity(name = "tokens")
@NoArgsConstructor
@Getter
public class ActionToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String token;
    private Instant expires;

    @Enumerated(EnumType.STRING)
    private TokenType type;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    public ActionToken(Instant expires, TokenType type, User user) {
        this.expires = expires;
        this.type = type;
        this.user = user;
    }
}
