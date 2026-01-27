package com.ifride.core.driver.service;

import com.ifride.core.auth.service.UserService;
import com.ifride.core.driver.model.dto.DriverApplicationRejectionDTO;
import com.ifride.core.driver.model.dto.DriverApplicationRequestDTO;
import com.ifride.core.driver.model.dto.DriverApplicationSummaryDTO;
import com.ifride.core.driver.model.entity.DriverApplication;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.driver.model.enums.DriverApplicationStatus;
import com.ifride.core.driver.repository.DriverApplicationRepository;
import com.ifride.core.driver.service.validators.DriverApplicationValidator;
import com.ifride.core.events.models.DriverApplicationApprovedEvent;
import com.ifride.core.shared.exceptions.api.NotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.ifride.core.driver.model.enums.DriverApplicationStatus.*;

@Service
@AllArgsConstructor
@Log4j2
public class DriverApplicationService {

    private final DriverApplicationRepository repository;
    private final UserService userService;

    private final ApplicationEventPublisher eventPublisher;
    private final DriverApplicationValidator driverApplicationValidator;


    public DriverApplicationSummaryDTO createDriverApplication(User author, User user, DriverApplicationRequestDTO dto) {
        var lastDriverApplication = getLastDriverApplicationByUser(user.getId());

        driverApplicationValidator.validateDriverApplication(author, user, lastDriverApplication);

        var driverApplication = new DriverApplication();
        driverApplication.setRequester(user);
        driverApplication.setCnhNumber(dto.cnhNumber());
        driverApplication.setCnhCategory(dto.cnhCategory());
        driverApplication.setCnhExpiration(dto.expiration());
        driverApplication.setApplicationStatus(PENDING);

        driverApplication = repository.save(driverApplication);

        return DriverApplicationSummaryDTO.fromEntity(driverApplication);
    }

    @Transactional
    public DriverApplicationSummaryDTO approveDriverApplication(User author, String userId) {
        var application = changeDriverApplicationStatus(userId, APPROVED, author, null);

        eventPublisher.publishEvent(new DriverApplicationApprovedEvent(application, author));
        return DriverApplicationSummaryDTO.fromEntity(application);
    }

    @Transactional
    public DriverApplicationSummaryDTO rejectDriverApplication(User author, String userId, DriverApplicationRejectionDTO dto) {
        var deniedApplication = changeDriverApplicationStatus(userId, DENIED, author, dto.rejectionReason());
        return DriverApplicationSummaryDTO.fromEntity(deniedApplication);
    }

    @Transactional

    public void delete(String applicationId, User author) {
        var application = repository.findById(applicationId).orElseThrow(() -> new NotFoundException("Não foi possível encontrar uma solicitação com o id %s", applicationId));
        driverApplicationValidator.checkOwnership(author, application.getRequester());
        repository.delete(application);
    }

    private DriverApplication changeDriverApplicationStatus(String userId, DriverApplicationStatus status, User author, String rejectionReason) {
        var driverApplication = getLastDriverApplicationByUser(userId).orElseThrow(() -> new NotFoundException("O usuário não possui nenhuma solicitação para alterar o status!"));

        driverApplicationValidator.validateDriverApplicationStatusChange(driverApplication, status);

        if(rejectionReason != null) {
            driverApplication.setRejectionReason(rejectionReason);
        }

        driverApplication.setReviewedBy(author);
        driverApplication.setApplicationStatus(status);
        return repository.save(driverApplication);
    }

    private Optional<DriverApplication> getLastDriverApplicationByUser(String userId) {
        var requester = userService.findById(userId);
        var applications = repository.findAllByRequesterOrderByCreatedAtDesc(requester);

        if(applications.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(applications.getFirst());
    }

}
