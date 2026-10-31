package com.ifride.core.driver.service;

import com.ifride.core.driver.model.dto.CnhSecurityNumberDTO;
import com.ifride.core.shared.services.DeterministicEncryptorService;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.driver.model.dto.DriverApplicationRejectionDTO;
import com.ifride.core.driver.model.dto.DriverApplicationRequestDTO;
import com.ifride.core.driver.model.dto.DriverApplicationSummaryDTO;
import com.ifride.core.driver.model.entity.DriverApplication;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.driver.model.enums.DriverApplicationStatus;
import com.ifride.core.driver.model.mapper.DriverMapper;
import com.ifride.core.driver.repository.DriverApplicationRepository;
import com.ifride.core.driver.repository.specs.DriverApplicationSpecs;
import com.ifride.core.driver.service.validators.DriverApplicationValidator;
import com.ifride.core.events.driver.model.DriverApplicationApprovedEvent;
import com.ifride.core.shared.exceptions.api.NotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.ifride.core.driver.model.enums.DriverApplicationStatus.*;

@Service
@AllArgsConstructor
@Log4j2
public class DriverApplicationService {

    private final DriverApplicationRepository repository;
    private final UserService userService;
    private final DriverMapper mapper;
    private final DeterministicEncryptorService crypto;

    private final ApplicationEventPublisher eventPublisher;
    private final DriverApplicationValidator driverApplicationValidator;


    public DriverApplicationSummaryDTO createDriverApplication(User author, User user, DriverApplicationRequestDTO dto) {
        var lastDriverApplication = getLastDriverApplicationByUser(user.getId());
        driverApplicationValidator.validateDriverApplication(author, user, lastDriverApplication, dto);

        var driverApplication = mapper.fromRequest(dto, author);
        driverApplication = repository.save(driverApplication);

        return mapper.toDto(driverApplication, author);
    }

    @Transactional
    public DriverApplicationSummaryDTO approveDriverApplication(User author, String userId) {
        var application = changeDriverApplicationStatus(userId, APPROVED, author, null);

        eventPublisher.publishEvent(new DriverApplicationApprovedEvent(application, author));
        return mapper.toDto(application, author);
    }

    @Transactional
    public DriverApplicationSummaryDTO rejectDriverApplication(User author, String userId, DriverApplicationRejectionDTO dto) {
        var deniedApplication = changeDriverApplicationStatus(userId, DENIED, author, dto.rejectionReason());

        return mapper.toDto(deniedApplication, author);
    }

    @Transactional
    public void delete(String applicationId, User author) {
        var application = repository.findById(applicationId).orElseThrow(() -> new NotFoundException("Não foi possível encontrar uma solicitação com o id %s", applicationId));
        driverApplicationValidator.checkOwnership(author, application.getRequester());
        repository.delete(application);
    }

    public CnhSecurityNumberDTO findByDriverApplication(String applicationId) {
        var application = repository.findById(applicationId).orElseThrow(() -> new NotFoundException("Não foi possível encontrar uma solicitação com o id %s", applicationId));
        if (application.getCnhSecurityNumberEncrypted() == null) {
            throw new NotFoundException("O código de segurança não está mais disponível para esta aplicação.");
        }
        return mapper.fromDriverApplication(application);
    }

    public DriverApplicationSummaryDTO findById(String applicationId, User author) {
        return repository.findById(applicationId).map(application -> mapper.toDto(application, author)).orElseThrow(() -> new NotFoundException("Não foi possível encontrar uma solicitação com o ID %s", applicationId));
    }

    public DriverApplicationSummaryDTO getMyApplication(User requester) {
        return getLastDriverApplicationByUser(requester.getId())
                .map(app -> mapper.toDto(app, requester))
                .orElseThrow(() -> new NotFoundException("Nenhuma solicitação encontrada para o usuário."));
    }

    public Page<DriverApplicationSummaryDTO> findBy(
            List<DriverApplicationStatus> statuses,
            String search,
            String cnh,
            Pageable pageable) {

        var cnhEncrypted = (cnh != null && !cnh.isBlank()) ? crypto.encrypt(cnh) : null;
        var spec = DriverApplicationSpecs.buildSearchSpec(statuses, search, cnhEncrypted);
        return repository.findAll(spec, pageable)
                .map(mapper::toListItem);
    }

    private DriverApplication changeDriverApplicationStatus(String userId, DriverApplicationStatus status, User author, String rejectionReason) {
        var driverApplication = getLastDriverApplicationByUser(userId).orElseThrow(() -> new NotFoundException("O usuário não possui nenhuma solicitação para alterar o status!"));

        driverApplicationValidator.validateDriverApplicationStatusChange(driverApplication, status);

        if(rejectionReason != null) {
            driverApplication.setRejectionReason(rejectionReason);
        }

        driverApplication.setReviewedBy(author);
        driverApplication.setApplicationStatus(status);
        driverApplication.setCnhSecurityNumberEncrypted(null);
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
