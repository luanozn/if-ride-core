package com.ifride.core.events.emails.listener;

import com.ifride.core.events.emails.model.ChangePasswordRequestedEvent;
import com.ifride.core.events.emails.model.EmailVerificationEvent;
import com.ifride.core.shared.services.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Log4j2
@Component
@RequiredArgsConstructor
public class AuthEmailEventListener {

    private final EmailService emailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetRequested(ChangePasswordRequestedEvent event) {
        try {
            emailService.sendPasswordChangeEmail(event.email(), event.plainToken());
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail de redefinição de senha para {}", event.email(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmailVerificationRequested(EmailVerificationEvent event) {
        try {
            emailService.sendEmailVerificationEmail(event.email(), event.plainToken());
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail de verificação para {}", event.email(), e);
        }
    }
}