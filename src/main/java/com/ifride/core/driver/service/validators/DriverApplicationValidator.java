package com.ifride.core.driver.service.validators;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.driver.model.entity.DriverApplication;
import com.ifride.core.driver.model.enums.DriverApplicationStatus;
import com.ifride.core.driver.repository.DriverApplicationRepository;
import com.ifride.core.shared.exceptions.api.ConflictException;
import com.ifride.core.shared.exceptions.api.ForbiddenException;
import com.ifride.core.shared.model.enums.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

import static com.ifride.core.driver.model.enums.DriverApplicationStatus.APPROVED;
import static com.ifride.core.driver.model.enums.DriverApplicationStatus.PENDING;

@Component
@RequiredArgsConstructor
public class DriverApplicationValidator {

    private final DriverApplicationRepository repository;

    public void validateDriverApplication(User author, User user, Optional<DriverApplication> driverApplication) {
        checkDriverExists(user);
        checkOwnership(author, user);
        checkDriverApplicationValidity(driverApplication);
    }

    public void validateDriverApplicationStatusChange(DriverApplication driverApplication, DriverApplicationStatus newStatus) {
        if(driverApplication.getApplicationStatus() != PENDING && newStatus != PENDING) {
            throw new ConflictException("Não é possível modificar uma requisição que está com o status %s, a menos que seja para voltá-la ao status PENDING", driverApplication.getApplicationStatus());
        }
    }

    public void checkOwnership(User author, User user) {
        if(userIsRequestingForOthers(author, user)) {
            throw new ForbiddenException("Somente o próprio usuário pode solicitar para virar motorista!");
        }
    }

    private boolean userIsRequestingForOthers(User author, User requested) {
        return !Objects.equals(author.getEmail(), requested.getEmail());
    }

    private void checkDriverExists(User user) {
        if(user.has(Role.DRIVER)) {
            throw new ConflictException("O usuário %s já é um MOTORISTA", user.getEmail());
        }
    }

    private void checkDriverApplicationValidity(Optional<DriverApplication> application) {
        application.ifPresent((driverApplication) -> {
            if(!driverApplication.getStatus().equals(Status.DELETED)) {
                checkDriverApplicationPending(driverApplication);
                checkDriverApplicationApproved(driverApplication);
            }
        });
    }

    private void checkDriverApplicationPending(DriverApplication driverApplication) {
        if(driverApplication.getApplicationStatus() == PENDING) {
            throw new ConflictException("O usuário %s ainda tem uma requisição pendente!", driverApplication.getRequester().getEmail());
        }
    }

    private void checkDriverApplicationApproved(DriverApplication driverApplication) {
        if(driverApplication.getApplicationStatus() == APPROVED) {
            throw new ConflictException("O usuário %s já tem uma requisição aprovada!", driverApplication.getRequester().getEmail());
        }
    }

}
