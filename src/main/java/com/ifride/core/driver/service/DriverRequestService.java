package com.ifride.core.driver.service;

import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.driver.model.dto.DriverRequestChangeStatusDTO;
import com.ifride.core.driver.model.dto.DriverRequestDTO;
import com.ifride.core.driver.model.entity.Driver;
import com.ifride.core.driver.model.entity.DriverRequest;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.driver.model.enums.DriverRequestStatus;
import com.ifride.core.driver.repository.DriverRequestRepository;
import com.ifride.core.shared.exceptions.api.ConflictException;
import com.ifride.core.shared.exceptions.api.ForbiddenException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static com.ifride.core.driver.model.enums.DriverRequestStatus.*;

@Service
@AllArgsConstructor
public class DriverRequestService {

    private final DriverRequestRepository repository;
    private final DriverService driverService;
    private final UserService userService;

    public DriverRequest createDriverRequest(User author, User user, DriverRequestDTO dto) {
        if(!userIsRequestingForHimself(author, user)) {
            throw new ForbiddenException("Somente o próprio usuário pode solicitar para virar motorista!");
        }

        if(user.has(Role.DRIVER)) {
            throw new ConflictException("O usuário %s já é um MOTORISTA", user.getEmail());
        }

        var driverRequest = new DriverRequest();
        driverRequest.setRequester(user);
        driverRequest.setCnhNumber(dto.cnhNumber());
        driverRequest.setCnhCategory(dto.cnhCategory());
        driverRequest.setCnhExpiration(dto.expiration());
        driverRequest.setStatus(PENDING);

        return repository.save(driverRequest);
    }

    public Driver approveDriveRequest(User author, String userId) {
        var driverRequest = changeDriverRequestStatus(userId, APPROVED, author, null);
        return driverService.saveFromDriverRequest(driverRequest);
    }

    public DriverRequest rejectDriveRequest(User author, String userId, DriverRequestChangeStatusDTO dto) {
        return changeDriverRequestStatus(userId, DENIED, author, dto.rejectionReason());
    }

    private DriverRequest changeDriverRequestStatus(String userId, DriverRequestStatus status, User author, String rejectionReason) {
        var driverRequest = getLastDriverRequestByUser(userId);

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

    private DriverRequest getLastDriverRequestByUser(String userId) {
        var requester = userService.findById(userId);

        return repository.findAllByRequesterOrderByCreatedAtDesc(requester).getFirst();
    }

    private boolean userIsRequestingForHimself(User author, User requested) {
        return !Objects.equals(author.getEmail(), requested.getEmail());
    }
}
