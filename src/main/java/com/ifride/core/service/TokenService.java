package com.ifride.core.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.ifride.core.model.auth.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    private final Long EXPIRATION = 21600L;
    private final Algorithm ALGORITHM = Algorithm.HMAC256(secret);
    private final String ISSUER = "if-ride-core";

    public String generateToken(User user) {
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(user.getEmail())
                .withExpiresAt(generateExpirationDate())
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
