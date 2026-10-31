package com.ifride.core.service.events;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.driver.model.entity.DriverApplication;
import com.ifride.core.driver.service.DriverService;
import com.ifride.core.events.driver.model.DriverApplicationApprovedEvent;
import com.ifride.core.events.driver.DriverApplicationListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DriverApplicationListenerTest {

    @Mock
    private DriverService driverService;

    @Mock
    private UserService userService;

    @InjectMocks
    private DriverApplicationListener listener;

    @Nested
    @DisplayName("Processamento de Aprovação de Motorista")
    class DriverApprovalProcessing {

        @Test
        @DisplayName("Deve criar o perfil de motorista e alterar a role do usuário ao receber o evento")
        void shouldCreateDriverProfileAndUpdateRole() {
            var requester = new User();
            requester.setId("user-1");

            var application = new DriverApplication();
            application.setRequester(requester);

            var event = new DriverApplicationApprovedEvent(application, new User());

            listener.handleDriverCreation(event);

            verify(driverService).saveFromDriverRequest(application);
            verify(userService).updateUserRole(requester, Role.DRIVER);
        }
    }
}