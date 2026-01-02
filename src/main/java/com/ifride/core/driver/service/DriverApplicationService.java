package com.ifride.core.driver.service;

import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.driver.model.dto.DriverApplicationRejectionDTO;
import com.ifride.core.driver.model.dto.DriverApplicationDTO;
import com.ifride.core.driver.model.entity.Driver;
import com.ifride.core.driver.model.entity.DriverApplication;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.driver.model.enums.DriverApplicationStatus;
import com.ifride.core.driver.repository.DriverApplicationRepository;
import com.ifride.core.driver.repository.DriverRepository;
import com.ifride.core.events.models.DriverApplicationApprovedEvent;
import com.ifride.core.shared.exceptions.api.ConflictException;
import com.ifride.core.shared.exceptions.api.ForbiddenException;
import com.ifride.core.shared.exceptions.api.NotFoundException;
import com.ifride.core.shared.model.enums.Status;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

import static com.ifride.core.driver.model.enums.DriverApplicationStatus.*;

@Service
@AllArgsConstructor
@Log4j2
public class DriverApplicationService {

    private final DriverApplicationRepository repository;
    private final UserService userService;

    private final ApplicationEventPublisher eventPublisher;


    public DriverApplication createDriverApplication(User author, User user, DriverApplicationDTO dto) {
        if(!userIsRequestingForHimself(author, user)) {
            throw new ForbiddenException("Somente o próprio usuário pode solicitar para virar motorista!");
        }

        if(user.has(Role.DRIVER)) {
            throw new ConflictException("O usuário %s já é um MOTORISTA", user.getEmail());
        }

        var lastDriverApplication = getLastDriverApplicationByUser(user.getId());
        validateLastDriverApplication(lastDriverApplication);

        var driverRequest = new DriverApplication();
        driverRequest.setRequester(user);
        driverRequest.setCnhNumber(dto.cnhNumber());
        driverRequest.setCnhCategory(dto.cnhCategory());
        driverRequest.setCnhExpiration(dto.expiration());
        driverRequest.setApplicationStatus(PENDING);

        return repository.save(driverRequest);
    }

    @Transactional
    public DriverApplication approveDriverApplication(User author, String userId) {
        var application = changeDriverApplicationStatus(userId, APPROVED, author, null);

        eventPublisher.publishEvent(new DriverApplicationApprovedEvent(application, author));
        return application;
    }

    public DriverApplication rejectDriverApplication(User author, String userId, DriverApplicationRejectionDTO dto) {
        return changeDriverApplicationStatus(userId, DENIED, author, dto.rejectionReason());
    }

    public void delete(String applicationId, User author) {
        var application = repository.findById(applicationId).orElseThrow(() -> new NotFoundException("Não foi possível encontrar uma solicitação com o id %s", applicationId));

        if(userIsRequestingForHimself(author, application.getRequester())) {
            repository.delete(application);
        } else {
            throw new ForbiddenException("O usuário %s não pode excluir solicitações de outros usuários!", author.getEmail());
        }
    }

    private DriverApplication changeDriverApplicationStatus(String userId, DriverApplicationStatus status, User author, String rejectionReason) {
        var driverRequest = getLastDriverApplicationByUser(userId).orElseThrow(() -> new NotFoundException("O usuário não possui nenhuma solicitação para alterar o status!"));

        if(driverRequest.getApplicationStatus() != PENDING && status != PENDING) {
            throw new ConflictException("Não é possível modificar uma requisição que está com o status %s", driverRequest.getApplicationStatus());
        }

        if(rejectionReason != null) {
            driverRequest.setRejectionReason(rejectionReason);
        }

        driverRequest.setReviewedBy(author);
        driverRequest.setApplicationStatus(status);
        return repository.save(driverRequest);
    }

    private Optional<DriverApplication> getLastDriverApplicationByUser(String userId) {
        var requester = userService.findById(userId);
        var applications = repository.findAllByRequesterOrderByCreatedAtDesc(requester);

        if(applications.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(applications.getFirst());
    }

    private boolean userIsRequestingForHimself(User author, User requested) {
        return Objects.equals(author.getEmail(), requested.getEmail());
    }

    private void validateLastDriverApplication(Optional<DriverApplication> application) {
        application.ifPresent((driverApplication) -> {
            if(!driverApplication.getStatus().equals(Status.DELETED)) {
                if(driverApplication.getApplicationStatus() == PENDING) {
                    throw new ConflictException("O usuário %s ainda tem uma requisição pendente!", driverApplication.getRequester().getEmail());
                }
                if(driverApplication.getApplicationStatus() == APPROVED) {
                    throw new ConflictException("O usuário %s já tem uma requisição aprovada!", driverApplication.getRequester().getEmail());
                }
            }
        });

    }
}
