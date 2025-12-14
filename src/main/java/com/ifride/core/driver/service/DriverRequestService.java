package com.ifride.core.driver.service;

import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.driver.model.dto.DriverRequestDTO;
import com.ifride.core.driver.model.entity.DriverRequest;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.driver.model.enums.DriverRequestStatus;
import com.ifride.core.driver.repository.DriverRequestRepository;
import com.ifride.core.shared.exceptions.api.ConflictException;
import com.ifride.core.shared.exceptions.api.ForbiddenException;
import com.ifride.core.shared.exceptions.api.NotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static com.ifride.core.driver.model.enums.DriverRequestStatus.PENDING;

@Service
@AllArgsConstructor
public class DriverRequestService {

    private final DriverRequestRepository repository;

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

    private DriverRequest changeDriverRequestStatus(String id, DriverRequestStatus status) {
        var driverRequest = repository.findById(id).orElseThrow(() -> new NotFoundException("Não foi possível encontrar a requisição no sistema."));

        if(driverRequest.getStatus() != PENDING) {
            throw new ConflictException("Não é possível aprovar uma requisição que está com o status %s", driverRequest.getStatus());
        }

        driverRequest.setStatus(status);
        return repository.save(driverRequest);
    }

    private boolean userIsRequestingForHimself(User author, User requested) {
        return !Objects.equals(author.getEmail(), requested.getEmail());
    }
}
