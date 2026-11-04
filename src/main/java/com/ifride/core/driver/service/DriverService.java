package com.ifride.core.driver.service;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.driver.model.entity.Driver;
import com.ifride.core.driver.model.entity.DriverApplication;
import com.ifride.core.driver.model.mapper.DriverMapper;
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
    private final DriverMapper mapper;


    public void saveFromDriverRequest(DriverApplication driverApplication) {
        var driver = mapper.fromApplication(driverApplication);
        User managedUser = entityManager.find(User.class, driverApplication.getRequester().getId());
        driver.setUser(managedUser);
        entityManager.persist(driver);
    }

    public Driver findById(String id) {
        return driverRepository.findById(id).orElseThrow(() -> new NotFoundException("O motorista com o ID %s não foi encontrado!", id));
    }
}
