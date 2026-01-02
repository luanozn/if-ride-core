package com.ifride.core.driver.controller;

import com.ifride.core.driver.model.dto.VehicleCreationDTO;
import com.ifride.core.driver.model.entity.Vehicle;
import com.ifride.core.driver.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/driver/{driverId}/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    public Vehicle create(@PathVariable String driverId, @RequestBody VehicleCreationDTO vehicle) {
        return vehicleService.saveBy(vehicle, driverId);
    }
}
