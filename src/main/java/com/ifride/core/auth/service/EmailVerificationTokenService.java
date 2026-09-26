package com.ifride.core.auth.service;

import com.ifride.core.auth.model.EmailVerificationMaxAttemptsExceededException;
import com.ifride.core.auth.model.entity.ActionToken;
import com.ifride.core.auth.model.entity.Attempt;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.TokenType;
import com.ifride.core.auth.repository.ActionTokenRepository;
import com.ifride.core.auth.service.interfaces.TokenManager;
import com.ifride.core.shared.exceptions.InvalidTokenTypeException;
import com.ifride.core.shared.exceptions.TokenExpiredException;
import com.ifride.core.shared.exceptions.TokenNotFoundException;
import com.ifride.core.shared.services.EmailService;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class EmailVerificationTokenService implements TokenManager {

    private final ActionTokenRepository tokenRepository;
    private final UserService userService;
    private final EmailService emailService;
    private final ConcurrentHashMap<String, Attempt> emailVerificationAttempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(15);
    private static final Duration TOKEN_TTL = Duration.ofHours(24);

    @Override
    public void generateTokenAndSendEmail(User user) {
        enforceRateLimit(user);

        var existingToken = tokenRepository.findByUserAndType(user, TokenType.EMAIL_VERIFICATION);
        ActionToken token = resolveToken(existingToken, user);

        tokenRepository.save(token);

        emailService.sendEmailVerificationEmail(user.getEmail(), token.getToken());
    }

    private ActionToken resolveToken(Optional<ActionToken> existing, User user) {
        if (existing.isPresent() && existing.get().getExpires().isAfter(Instant.now())) {
            log.info("Reutilizando token existente para {} (expira em {})", user.getEmail(), existing.get().getExpires());
            return existing.get();
        } else if (existing.isPresent() && existing.get().getExpires().isBefore(Instant.now())) {
            tokenRepository.delete(existing.get());
        }

        return new ActionToken(
                Instant.now().plus(TOKEN_TTL),
                TokenType.EMAIL_VERIFICATION,
                user
        );
    }

    private void enforceRateLimit(User user) {
        var now = Instant.now();
        emailVerificationAttempts.compute(user.getId(), (id, current) -> {
            if (current == null || current.lastAttempt().isBefore(now.minus(ATTEMPT_WINDOW))) {
                return new Attempt(1, now);
            }
            if (current.quantity() >= MAX_ATTEMPTS) {
                throw new EmailVerificationMaxAttemptsExceededException(
                        "Limite atingido para: " + user.getEmail()
                );
            }
            return new Attempt(current.quantity() + 1, now);
        });
    }

    @Override
    public void confirmEmailVerification(String token) {
        var validToken = validateToken(token);
        verifyUserEmail(validToken);
    }

    private void verifyUserEmail(ActionToken token) {
        var user = token.getUser();
        user.setEmailVerified(true);
        tokenRepository.delete(token);

        userService.save(user);
    }

    private ActionToken validateToken(String token) {
        var foundToken = tokenRepository.findById(token).orElseThrow(() -> new TokenNotFoundException("O token inserido não foi encontrado!"));

        if (foundToken.getType() != TokenType.EMAIL_VERIFICATION) {
            throw new InvalidTokenTypeException("O token encontrado não é de verificação de email!");
        }

        if (foundToken.getExpires().isBefore(Instant.now())) {
            throw new TokenExpiredException("O token está expirado!");
        }

        return foundToken;
    }
}
