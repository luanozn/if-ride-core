package com.ifride.core.driver.service;

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


    public void saveFromDriverRequest(DriverApplication driverApplication) {
        Driver driver = new Driver();
        driver.setId(driverApplication.getRequester().getId());
        driver.setUser(driverApplication.getRequester());
        driver.setCnhNumber(driverApplication.getCnhNumber());
        driver.setCnhCategory(driverApplication.getCnhCategory());
        driver.setCnhExpiration(driverApplication.getCnhExpiration());

        entityManager.persist(driver);
    }

    public Driver findById(String id) {
        return driverRepository.findById(id).orElseThrow(() -> new NotFoundException("O motorista com o ID %s não foi encontrado!", id));
    }
}
