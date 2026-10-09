package com.ifride.core.driver.listeners;

import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.driver.service.DriverService;
import com.ifride.core.events.models.DriverApplicationApprovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class DriverApplicationListener {
    private final DriverService driverService;
    private final UserService userService;

    @EventListener
    public void handleDriverCreation(DriverApplicationApprovedEvent event) {
        log.info("Creating driver for event: {}", event.toString());
        driverService.saveFromDriverRequest(event.driverApplication());
        log.info("Driver created: {}", event.driverApplication().getRequester().getId());
    }

    @EventListener
    public void handleRoleChange(DriverApplicationApprovedEvent event) {
        log.info("Changing user role for event: {}", event.toString());
        userService.updateUserRole(event.driverApplication().getRequester(), Role.DRIVER);
        log.info("User {} role successfully changed to {}", event.driverApplication().getRequester().getId(), Role.DRIVER);
    }
}
