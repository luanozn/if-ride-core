package com.ifride.core.auth.service;

import com.ifride.core.auth.model.dto.ActionTokenWrapper;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.TokenType;
import com.ifride.core.auth.repository.ActionTokenRepository;
import com.ifride.core.auth.service.abstracts.TokenManager;
import com.ifride.core.shared.services.EmailService;
import com.ifride.core.shared.services.HashingService;
import java.time.Clock;
import java.time.Duration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ChangePasswordTokenService extends TokenManager {

    private final UserService userService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private static final Integer MAX_ATTEMPTS = 3;
    private static final Integer ATTEMPT_WINDOW = 15;
    private static final Duration TOKEN_TTL = Duration.ofMinutes(15);

    ChangePasswordTokenService(ActionTokenRepository tokenRepository, UserService userService, EmailService emailService, HashingService hashingService, Clock clock, PasswordEncoder passwordEncoder) {
        super(MAX_ATTEMPTS, ATTEMPT_WINDOW, TOKEN_TTL, clock, tokenRepository, hashingService);
        this.userService = userService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void generateTokenAndSendEmail(User user) {
        enforceRateLimit(user.getId(), TokenType.PASSWORD_CHANGE);
        tokenRepository.findByUserAndType(user, TokenType.PASSWORD_CHANGE)
                .ifPresent(tokenRepository::delete);

        ActionTokenWrapper wrapper = generateActionToken(user, TokenType.PASSWORD_CHANGE);

        tokenRepository.save(wrapper.getToken());
        emailService.sendPasswordChangeEmail(user.getEmail(), wrapper.getPlainToken());
    }

    public void resetPassword(String plainToken, String newPassword) {
        var validToken = validateToken(TokenType.PASSWORD_CHANGE, plainToken);

        var user = validToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userService.save(user);

        tokenRepository.delete(validToken);
    }
}
