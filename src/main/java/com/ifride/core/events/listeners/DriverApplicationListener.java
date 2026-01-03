package com.ifride.core.events.listeners;

import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.driver.service.DriverService;
import com.ifride.core.events.models.DriverApplicationApprovedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DriverApplicationListener {
    private final DriverService driverService;
    private final UserService userService;

    @EventListener
    public void handleDriverCreation(DriverApplicationApprovedEvent event) {
        driverService.saveFromDriverRequest(event.driverApplication());
    }

    @EventListener
    public void handleRoleChange(DriverApplicationApprovedEvent event) {
        userService.updateUserRole(event.driverApplication().getRequester(), Role.DRIVER);
    }
}
