package com.ifride.core.service.events.user.listener;

import static org.mockito.Mockito.verify;

import com.ifride.core.auth.repository.ActionTokenRepository;
import com.ifride.core.auth.service.EmailVerificationTokenService;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.chat.repository.ChatMessageRepository;
import com.ifride.core.chat.repository.ConversationRepository;
import com.ifride.core.driver.service.DriverApplicationService;
import com.ifride.core.driver.service.DriverService;
import com.ifride.core.driver.service.VehicleService;
import com.ifride.core.events.user.listener.UserListener;
import com.ifride.core.events.user.model.UserDeletedEvent;
import com.ifride.core.shared.audit.service.AuditLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserListenerTest {

    @Mock private UserService userService;
    @Mock private DriverService driverService;
    @Mock private EmailVerificationTokenService tokenService;
    @Mock private AuditLogService auditLogService;
    @Mock private VehicleService vehicleService;
    @Mock private ActionTokenRepository actionTokenRepository;
    @Mock private DriverApplicationService driverApplicationService;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ConversationRepository conversationRepository;

    @InjectMocks private UserListener listener;

    @Test
    @DisplayName("Exclui dados vinculados e anonimiza mensagens e nomes das conversas")
    void deletionCleansAssociatedDataAndAnonymizesChat() {
        String userId = "user-1";

        listener.handleUserDeletedEvent(new UserDeletedEvent(userId));

        verify(driverService).deleteIfPresent(userId);
        verify(vehicleService).deleteByOwner(userId);
        verify(actionTokenRepository).deleteAllByUserId(userId);
        verify(driverApplicationService).deleteIfPresentByUser(userId);
        verify(chatMessageRepository).anonymizeSenderName(userId);
        verify(conversationRepository).anonymizeNames(userId);
    }
}
