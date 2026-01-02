package com.ifride.core.driver.service;

import com.ifride.core.driver.model.dto.VehicleCreationDTO;
import com.ifride.core.driver.model.entity.Vehicle;
import com.ifride.core.driver.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository repository;
    private final DriverService driverService;

    public Vehicle saveBy(VehicleCreationDTO dto, String driverId) {
        var vehicle = new Vehicle();

        var owner = driverService.findById(driverId);

        vehicle.setModel(dto.model());
        vehicle.setColor(dto.color());
        vehicle.setPlate(dto.plate());
        vehicle.setOwner(owner);

        return repository.save(vehicle);
    }
}
