package com.ifride.core.events.user.listener;

import com.ifride.core.auth.service.EmailVerificationTokenService;
import com.ifride.core.events.user.model.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserRegistrationListener {

    private final EmailVerificationTokenService tokenService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        var user = event.user();
        tokenService.generateTokenAndSendEmail(user);
    }
}