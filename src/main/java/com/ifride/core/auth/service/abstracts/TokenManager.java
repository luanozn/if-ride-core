package com.ifride.core.auth.service.abstracts;

import com.ifride.core.auth.model.dto.ActionTokenWrapper;
import com.ifride.core.auth.model.entity.ActionToken;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.TokenType;
import com.ifride.core.auth.repository.ActionTokenRepository;
import com.ifride.core.shared.exceptions.InvalidTokenTypeException;
import com.ifride.core.shared.exceptions.TokenExpiredException;
import com.ifride.core.shared.exceptions.TokenNotFoundException;
import com.ifride.core.shared.rate_limit.RateLimitOperation;
import com.ifride.core.shared.rate_limit.RateLimitService;
import com.ifride.core.shared.services.HashingService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public abstract class TokenManager {
    private final Integer maxAttempts;
    private final Duration attemptWindow;
    protected final Duration tokenTtl;

    protected final ActionTokenRepository tokenRepository;
    protected final HashingService hashingService;
    protected final RateLimitService rateLimitService;
    protected final Clock clock;

    public TokenManager(Integer maxAttempts, Integer attemptWindowInMinutes, Duration tokenTtlInHours, Clock clock, ActionTokenRepository tokenRepository, HashingService hashingService, RateLimitService rateLimitService) {
        this.maxAttempts = maxAttempts;
        this.attemptWindow = Duration.ofMinutes(attemptWindowInMinutes);
        this.tokenTtl = tokenTtlInHours;

        this.clock = clock;
        this.tokenRepository = tokenRepository;
        this.hashingService = hashingService;
        this.rateLimitService = rateLimitService;
    }

    protected abstract void generateTokenAndSendEmail(User user);

    public ActionTokenWrapper generateActionToken(User user, TokenType type) {
        String plainToken = UUID.randomUUID().toString();
        String hashedToken = hashingService.hash(plainToken);

        ActionToken token = new ActionToken(
                hashedToken,
                Instant.now(clock).plus(this.tokenTtl),
                type,
                user
        );

        return new ActionTokenWrapper(token, plainToken);
    }

    protected void enforceRateLimit(User user, RateLimitOperation operation) {
        rateLimitService.checkLimit(operation, user.getId(), this.maxAttempts, this.attemptWindow);
    }

    protected ActionToken validateToken(TokenType type, String plainToken) {
        String hashedToken = hashingService.hash(plainToken);
        var foundToken = tokenRepository.findById(hashedToken)
                .orElseThrow(() -> new TokenNotFoundException("O token inserido não foi encontrado!"));

        if (foundToken.getType() != type) {
            throw new InvalidTokenTypeException("O token encontrado não é do tipo esperado!");
        }

        if (foundToken.getExpires().isBefore(Instant.now(clock))) {
            throw new TokenExpiredException("O token está expirado!");
        }

        return foundToken;
    }
}
