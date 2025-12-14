package com.ifride.core.driver.service;

import com.ifride.core.driver.model.entity.Driver;
import com.ifride.core.driver.model.entity.DriverRequest;
import com.ifride.core.driver.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;

    public Driver saveFromDriverRequest(DriverRequest driverRequest) {
        Driver driver = new Driver();
        driver.setUser(driverRequest.getRequester());
        driver.setCnhNumber(driverRequest.getCnhNumber());
        driver.setCnhCategory(driverRequest.getCnhCategory());
        driver.setCnhExpiration(driverRequest.getCnhExpiration());

        return driverRepository.save(driver);
    }
}
