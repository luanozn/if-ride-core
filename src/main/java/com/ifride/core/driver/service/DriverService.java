package com.ifride.core.driver.service;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.repository.UserRepository;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.driver.model.dto.DriverApplicationRequestDTO;
import com.ifride.core.driver.model.entity.Driver;
import com.ifride.core.driver.model.entity.DriverApplication;
import com.ifride.core.driver.repository.DriverRepository;
import com.ifride.core.shared.exceptions.api.NotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DriverService {

    @PersistenceContext
    private EntityManager entityManager;
    private final DriverRepository driverRepository;
    private final UserService userService;


    public void saveFromDriverRequest(DriverApplication driverApplication) {
        Driver driver = new Driver();
        driver.setId(driverApplication.getRequester().getId());
        driver.setUser(driverApplication.getRequester());
        driver.setCnhNumber(driverApplication.getCnhNumber());
        driver.setCnhCategory(driverApplication.getCnhCategory());
        driver.setCnhExpiration(driverApplication.getCnhExpiration());

        entityManager.persist(driver);
    }

    public void saveFromDTO(DriverApplicationRequestDTO driverApplicationRequestDTO) {
        User user = userService.findById(driverApplicationRequestDTO.requesterId());
        Driver driver = new Driver();
        driver.setUser(user);
        driver.setCnhNumber(driverApplicationRequestDTO.cnhNumber());
        driver.setCnhCategory(driverApplicationRequestDTO.cnhCategory());
        driver.setCnhExpiration(driverApplicationRequestDTO.expiration());

        driverRepository.save(driver);
    }

    public Driver findById(String id) {
        return driverRepository.findById(id).orElseThrow(() -> new NotFoundException("O motorista com o ID %s não foi encontrado!", id));
    }
}
