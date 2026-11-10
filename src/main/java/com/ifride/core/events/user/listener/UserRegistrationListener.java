package com.ifride.core.events.user.listener;

import com.ifride.core.auth.service.EmailVerificationTokenService;
import com.ifride.core.events.user.model.UserRegisteredEvent;
import com.ifride.core.shared.audit.enums.AuditAction;
import com.ifride.core.shared.audit.enums.TargetType;
import com.ifride.core.shared.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserRegistrationListener {

    private final EmailVerificationTokenService tokenService;
    private final AuditLogService auditLogService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        var user = event.user();
        tokenService.generateTokenAndSendEmail(user);

        auditLogService.logAction(
                AuditAction.SEND,
                event.user().getId(),
                TargetType.EMAIL_VERIFICATION,
                "SYSTEM",
                null
        );
    }
}