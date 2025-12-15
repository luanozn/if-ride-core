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
import com.ifride.core.shared.exceptions.api.ConflictException;
import com.ifride.core.shared.exceptions.api.ForbiddenException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static com.ifride.core.driver.model.enums.DriverApplicationStatus.*;

@Service
@AllArgsConstructor
public class DriverApplicationService {

    private final DriverApplicationRepository repository;
    private final DriverService driverService;
    private final UserService userService;

    public DriverApplication createDriverApplication(User author, User user, DriverApplicationDTO dto) {
        if(!userIsRequestingForHimself(author, user)) {
            throw new ForbiddenException("Somente o próprio usuário pode solicitar para virar motorista!");
        }

        if(user.has(Role.DRIVER)) {
            throw new ConflictException("O usuário %s já é um MOTORISTA", user.getEmail());
        }

        var driverRequest = new DriverApplication();
        driverRequest.setRequester(user);
        driverRequest.setCnhNumber(dto.cnhNumber());
        driverRequest.setCnhCategory(dto.cnhCategory());
        driverRequest.setCnhExpiration(dto.expiration());
        driverRequest.setStatus(PENDING);

        return repository.save(driverRequest);
    }

    public Driver approveDriverApplication(User author, String userId) {
        var driverRequest = changeDriverApplicationStatus(userId, APPROVED, author, null);
        return driverService.saveFromDriverRequest(driverRequest);
    }

    public DriverApplication rejectDriverApplication(User author, String userId, DriverApplicationRejectionDTO dto) {
        return changeDriverApplicationStatus(userId, DENIED, author, dto.rejectionReason());
    }

    private DriverApplication changeDriverApplicationStatus(String userId, DriverApplicationStatus status, User author, String rejectionReason) {
        var driverRequest = getLastDriverApplicationByUser(userId);

        if(driverRequest.getStatus() != PENDING) {
            throw new ConflictException("Não é possível modificar uma requisição que está com o status %s", driverRequest.getStatus());
        }

        if(rejectionReason != null) {
            driverRequest.setRejectionReason(rejectionReason);
        }

        driverRequest.setReviewedBy(author);
        driverRequest.setStatus(status);
        return repository.save(driverRequest);
    }

    private DriverApplication getLastDriverApplicationByUser(String userId) {
        var requester = userService.findById(userId);

        return repository.findAllByRequesterOrderByCreatedAtDesc(requester).getFirst();
    }

    private boolean userIsRequestingForHimself(User author, User requested) {
        return !Objects.equals(author.getEmail(), requested.getEmail());
    }
}
