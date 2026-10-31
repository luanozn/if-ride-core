package com.ifride.core.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.ifride.core.auth.model.dto.LoginResponseDTO;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.shared.exceptions.api.ForbiddenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class JwtService {

    private final Long EXPIRATION = 21600L;
    private final Algorithm ALGORITHM;
    private final String ISSUER = "if-ride-core";

    public JwtService(@Value("${api.security.token.secret}") String secret) {
        this.ALGORITHM = Algorithm.HMAC256(secret);
    }

    public LoginResponseDTO generateLoginResponse(User user) {
        var expirationDate = generateExpirationDate();
        var token = generateToken(user, expirationDate);
        return new LoginResponseDTO(token, user.getEmail(), expirationDate.toEpochMilli(), user.getId(), user.getName());
    }

    public String generateToken(User user, Instant expirationDate) {
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(user.getEmail())
                .withExpiresAt(expirationDate)
                .withClaim("role", user.getRole().getRole())
                .sign(ALGORITHM);
    }

    public String validateToken(String token) {
        try {
            return JWT.require(ALGORITHM)
                    .withIssuer(ISSUER)
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTDecodeException e) {
            throw new ForbiddenException(e.getMessage());
        }
    }

    public Instant generateExpirationDate() {
        return Instant.now().plusSeconds(EXPIRATION);
    }
}
