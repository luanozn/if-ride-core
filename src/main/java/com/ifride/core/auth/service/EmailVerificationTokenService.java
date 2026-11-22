package com.ifride.core.auth.service;

import com.ifride.core.auth.model.dto.ActionTokenWrapper;
import com.ifride.core.auth.model.entity.ActionToken;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.TokenType;
import com.ifride.core.auth.repository.ActionTokenRepository;
import com.ifride.core.auth.service.abstracts.TokenManager;
import com.ifride.core.shared.services.EmailService;
import com.ifride.core.shared.services.HashingService; // <-- IMPORT AQUI
import jakarta.transaction.Transactional;

import java.time.Clock;
import java.time.Duration;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;


@Log4j2
@Service
@Transactional
public class EmailVerificationTokenService extends TokenManager {

    private final UserService userService;
    private final EmailService emailService;
    private static final Integer MAX_ATTEMPTS = 3;
    private static final Integer ATTEMPT_WINDOW = 15;
    private static final Duration TOKEN_TTL = Duration.ofHours(24);

    EmailVerificationTokenService(ActionTokenRepository tokenRepository, UserService userService, EmailService emailService, HashingService hashingService, Clock clock) {
        super(MAX_ATTEMPTS, ATTEMPT_WINDOW, TOKEN_TTL, clock, tokenRepository, hashingService);
        this.userService = userService;
        this.emailService = emailService;
    }

    public void generateTokenAndSendEmail(User user) {
        enforceRateLimit(user.getId(), TokenType.EMAIL_VERIFICATION);
        tokenRepository.findByUserAndType(user, TokenType.EMAIL_VERIFICATION)
                .ifPresent(tokenRepository::delete);

        ActionTokenWrapper wrapper = generateActionToken(user, TokenType.EMAIL_VERIFICATION);

        tokenRepository.save(wrapper.getToken());
        emailService.sendEmailVerificationEmail(user.getEmail(), wrapper.getPlainToken());
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