package com.ifride.core.driver.service;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.driver.model.entity.Driver;
import com.ifride.core.driver.model.entity.DriverApplication;
import com.ifride.core.driver.model.enums.CnhCategory;
import com.ifride.core.driver.model.mapper.DriverMapper;
import com.ifride.core.driver.repository.DriverRepository;
import com.ifride.core.shared.exceptions.api.NotFoundException;
import com.ifride.core.shared.services.DeterministicEncryptorService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DriverService {

    @PersistenceContext
    private EntityManager entityManager;
    private final DeterministicEncryptorService cryptoService;
    private final DriverRepository driverRepository;
    private final DriverMapper mapper;

    @Transactional
    public void saveFromDriverRequest(DriverApplication driverApplication) {
        var driver = mapper.fromApplication(driverApplication);
        User managedUser = entityManager.find(User.class, driverApplication.getRequester().getId());
        driver.setUser(managedUser);
        entityManager.persist(driver);
    }

    @Transactional
    public void deleteIfPresent(String id) {
        driverRepository.findById(id).ifPresent(present -> {
            present.setCnhNumberEncrypted(cryptoService.encrypt(UUID.randomUUID().toString()));
            present.setCnhCategory(CnhCategory.DELETED);
            present.setCnhExpiration(LocalDate.of(1900, 1, 1));
            driverRepository.save(present);

            driverRepository.delete(present);
        });
    }

    public Driver findById(String id) {
        return driverRepository.findById(id).orElseThrow(() -> new NotFoundException("O motorista com o ID %s não foi encontrado!", id));
    }
}
