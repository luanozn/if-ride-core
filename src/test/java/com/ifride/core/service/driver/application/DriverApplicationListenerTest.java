package com.ifride.core.service.driver.application;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.driver.listeners.DriverApplicationListener;
import com.ifride.core.driver.listeners.model.DriverApplicationApprovedEvent;
import com.ifride.core.driver.model.entity.DriverApplication;
import com.ifride.core.driver.service.DriverService;
import org.junit.jupiter.api.DisplayName;
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

    @Test
    @DisplayName("Deve criar o perfil de motorista ao receber evento de aprovação")
    void shouldCreateDriverOnApprovalEvent() {
        var requester = new User();
        var application = new DriverApplication();
        application.setRequester(requester);
        var event = new DriverApplicationApprovedEvent(application, new User());

        listener.handleDriverCreation(event);

        verify(driverService).saveFromDriverRequest(application);
    }

    @Test
    @DisplayName("Deve alterar a role do usuário para DRIVER ao receber evento")
    void shouldChangeRoleOnApprovalEvent() {
        var requester = new User();
        var application = new DriverApplication();
        application.setRequester(requester);
        var event = new DriverApplicationApprovedEvent(application, new User());

        listener.handleRoleChange(event);

        verify(userService).updateUserRole(requester, Role.DRIVER);
    }
}