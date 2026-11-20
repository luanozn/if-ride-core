package com.ifride.core.events.user.listener;

import com.ifride.core.auth.repository.ActionTokenRepository;
import com.ifride.core.auth.service.EmailVerificationTokenService;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.chat.repository.ChatMessageRepository;
import com.ifride.core.chat.repository.ConversationRepository;
import com.ifride.core.driver.service.DriverApplicationService;
import com.ifride.core.driver.service.DriverService;
import com.ifride.core.driver.service.VehicleService;
import com.ifride.core.events.user.model.UserDeletedEvent;
import com.ifride.core.events.user.model.UserRegisteredEvent;
import com.ifride.core.shared.audit.enums.AuditAction;
import com.ifride.core.shared.audit.enums.TargetType;
import com.ifride.core.shared.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Log4j2
public class UserListener {

    private final UserService userService;
    private final DriverService driverService;
    private final EmailVerificationTokenService tokenService;
    private final AuditLogService auditLogService;
    private final VehicleService vehicleService;
    private final ActionTokenRepository actionTokenRepository;
    private final DriverApplicationService driverApplicationService;
    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        var user = userService.findById(event.userId());
        tokenService.generateTokenAndSendEmail(user);

        auditLogService.logAction(
                AuditAction.SEND,
                user.getId(),
                TargetType.EMAIL_VERIFICATION,
                "SYSTEM",
                null
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleUserDeletedEvent(UserDeletedEvent event) {
        log.info("Deletando dados de motorista vinculados ao usuário {}", event.userId());
        driverService.deleteIfPresent(event.userId());
        log.info("Deletando dados de veículos vinculados ao usuário {}", event.userId());
        vehicleService.deleteByOwner(event.userId());
        log.info("Deletando tokens vinculados ao usuário {}", event.userId());
        actionTokenRepository.deleteAllByUserId(event.userId());
        log.info("Deletando aplicações de motorista vinculados ao usuário {}", event.userId());
        driverApplicationService.deleteIfPresentByUser(event.userId());

        log.info("Anonimizando nomes nas mensagens enviadas pelo usuário {}", event.userId());
        chatMessageRepository.anonymizeSenderName(event.userId());

        log.info("Anonimizando conversas em que o usuário {} participa", event.userId());
        conversationRepository.anonymizeNames(event.userId());


    }
}