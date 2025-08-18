package com.ifride.core.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.ifride.core.auth.model.dto.LoginResponseDTO;
import com.ifride.core.auth.model.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TokenService {

    private final Long EXPIRATION = 21600L;
    private final Algorithm ALGORITHM;
    private final String ISSUER = "if-ride-core";

    public TokenService(@Value("${api.security.token.secret}") String secret) {
        this.ALGORITHM = Algorithm.HMAC256(secret);
    }

    public LoginResponseDTO generateLoginResponse(User user) {
        var expirationDate = generateExpirationDate();
        var token = generateToken(user, expirationDate);
        return new LoginResponseDTO(token, user.getEmail(), expirationDate.toEpochMilli());
    }

    public String generateToken(User user, Instant expirationDate) {
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(user.getEmail())
                .withExpiresAt(expirationDate)
                .sign(ALGORITHM);
    }

    public String validateToken(String token) {
        return JWT.require(ALGORITHM)
                .withIssuer(ISSUER)
                .build()
                .verify(token)
                .getSubject();
    }

    public Instant generateExpirationDate() {
        return Instant.now().plusSeconds(EXPIRATION);
    }
}
