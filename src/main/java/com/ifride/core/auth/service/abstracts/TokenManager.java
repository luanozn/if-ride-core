package com.ifride.core.auth.service.abstracts;

import com.ifride.core.auth.model.TooManyRequestsException;
import com.ifride.core.auth.model.dto.ActionTokenWrapper;
import com.ifride.core.auth.model.entity.ActionToken;
import com.ifride.core.auth.model.entity.Attempt;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.TokenType;
import com.ifride.core.auth.repository.ActionTokenRepository;
import com.ifride.core.shared.exceptions.InvalidTokenTypeException;
import com.ifride.core.shared.exceptions.TokenExpiredException;
import com.ifride.core.shared.exceptions.TokenNotFoundException;
import com.ifride.core.shared.services.HashingService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class TokenManager {
    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Integer maxAttempts;
    private final Duration attemptWindow;
    protected final Duration tokenTtl;

    protected final ActionTokenRepository tokenRepository;
    protected final HashingService hashingService;
    protected final Clock clock;

    public TokenManager(Integer maxAttempts, Integer attemptWindowInMinutes, Duration tokenTtlInHours, Clock clock, ActionTokenRepository tokenRepository, HashingService hashingService) {
        this.maxAttempts = maxAttempts;
        this.attemptWindow = Duration.ofMinutes(attemptWindowInMinutes);
        this.tokenTtl = tokenTtlInHours;

        this.clock = clock;
        this.tokenRepository = tokenRepository;
        this.hashingService = hashingService;
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

    protected void enforceRateLimit(String identifier, TokenType type) {
        var now = Instant.now(clock);
        attempts.compute(String.format("%s_%s", identifier, type.toString()), (id, current) -> {
            if (current == null || current.lastAttempt().isBefore(now.minus(attemptWindow))) {
                return new Attempt(1, now);
            }
            if (current.quantity() >= maxAttempts) {
                throw new TooManyRequestsException(
                        "Limite de emails enviados atingido para: %s, tente novamente após 15 minutos", identifier
                );
            }
            return new Attempt(current.quantity() + 1, now);
        });
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
