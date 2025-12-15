package com.ifride.core.driver.service;

import com.ifride.core.driver.model.entity.Driver;
import com.ifride.core.driver.model.entity.DriverApplication;
import com.ifride.core.driver.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;

    public Driver saveFromDriverRequest(DriverApplication driverApplication) {
        Driver driver = new Driver();
        driver.setUser(driverApplication.getRequester());
        driver.setCnhNumber(driverApplication.getCnhNumber());
        driver.setCnhCategory(driverApplication.getCnhCategory());
        driver.setCnhExpiration(driverApplication.getCnhExpiration());

        return driverRepository.save(driver);
    }
}
