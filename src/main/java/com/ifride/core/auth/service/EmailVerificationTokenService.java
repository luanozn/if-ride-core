package com.ifride.core.auth.service;

import com.ifride.core.auth.model.entity.ActionToken;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.TokenType;
import com.ifride.core.auth.repository.ActionTokenRepository;
import com.ifride.core.auth.service.interfaces.TokenManager;
import com.ifride.core.shared.exceptions.InvalidTokenTypeException;
import com.ifride.core.shared.exceptions.TokenExpiredException;
import com.ifride.core.shared.exceptions.TokenNotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@Transactional
@AllArgsConstructor
public class EmailVerificationTokenService implements TokenManager {

    private final ActionTokenRepository tokenRepository;
    private final UserService userService;

    @Override
    public ActionToken generateToken(User user) {
        tokenRepository.deleteByUserAndType(user, TokenType.EMAIL_VERIFICATION);

        ActionToken token = new ActionToken(
                Instant.now().plus(24, ChronoUnit.HOURS),
                TokenType.EMAIL_VERIFICATION,
                user
        );
        return tokenRepository.save(token);
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

        if(foundToken.getType() != TokenType.EMAIL_VERIFICATION) {
            throw new InvalidTokenTypeException("O token encontrado não é de verificação de email!");
        }

        if(foundToken.getExpires().isBefore(Instant.now())) {
            throw new TokenExpiredException("O token está expirado!");
        }

        return foundToken;
    }
}
