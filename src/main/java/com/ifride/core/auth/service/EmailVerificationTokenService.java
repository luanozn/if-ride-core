package com.ifride.core.auth.service;

import com.ifride.core.auth.model.dto.ActionTokenWrapper;
import com.ifride.core.auth.model.entity.ActionToken;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.TokenType;
import com.ifride.core.auth.repository.ActionTokenRepository;
import com.ifride.core.auth.service.abstracts.TokenManager;
import com.ifride.core.events.emails.model.EmailVerificationEvent;
import com.ifride.core.shared.rate_limit.RateLimitOperation;
import com.ifride.core.shared.rate_limit.RateLimitService;
import com.ifride.core.shared.services.HashingService;
import jakarta.transaction.Transactional;

import java.time.Clock;
import java.time.Duration;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;


@Log4j2
@Service
@Transactional
public class EmailVerificationTokenService extends TokenManager {

    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private static final Integer MAX_ATTEMPTS = 3;
    private static final Integer ATTEMPT_WINDOW = 15;
    private static final Duration TOKEN_TTL = Duration.ofHours(24);

    public EmailVerificationTokenService(ActionTokenRepository tokenRepository, UserService userService, HashingService hashingService, Clock clock, RateLimitService rateLimitService, ApplicationEventPublisher eventPublisher) {
        super(MAX_ATTEMPTS, ATTEMPT_WINDOW, TOKEN_TTL, clock, tokenRepository, hashingService, rateLimitService);
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    public void generateTokenAndSendEmail(User user) {
        if(user.isEmailVerified()) {
            log.info("O usuário já possui o email verificado!");
            return;
        }
        enforceRateLimit(user, RateLimitOperation.EMAIL_VERIFICATION);
        tokenRepository.findByUserAndType(user, TokenType.EMAIL_VERIFICATION)
                .ifPresent(tokenRepository::delete);

        ActionTokenWrapper wrapper = generateActionToken(user, TokenType.EMAIL_VERIFICATION);

        tokenRepository.save(wrapper.getToken());
        eventPublisher.publishEvent(new EmailVerificationEvent(user.getEmail(), wrapper.getPlainToken()));
    }

    public void confirmEmailVerification(String token) {
        var validToken = validateToken(TokenType.EMAIL_VERIFICATION, token);
        verifyUserEmail(validToken);
    }

    private void verifyUserEmail(ActionToken token) {
        var user = token.getUser();
        user.setEmailVerified(true);
        tokenRepository.delete(token);

        userService.save(user);
    }
}