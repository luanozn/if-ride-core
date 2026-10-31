package com.ifride.core.events.driver;

import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.driver.service.DriverService;
import com.ifride.core.events.driver.model.DriverApplicationApprovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Log4j2
@Component
@RequiredArgsConstructor
public class DriverApplicationListener {
    private final DriverService driverService;
    private final UserService userService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    @Transactional(propagation = REQUIRES_NEW)
    public void handleDriverCreation(DriverApplicationApprovedEvent event) {
        log.info("Creating driver for event: {}", event.toString());
        driverService.saveFromDriverRequest(event.driverApplication());
        log.info("Driver created: {}", event.driverApplication().getRequester().getId());

        log.info("Changing user role for event: {}", event.toString());
        userService.updateUserRole(event.driverApplication().getRequester(), Role.DRIVER);
        log.info("User {} role successfully changed to {}", event.driverApplication().getRequester().getId(), Role.DRIVER);
    }
}
