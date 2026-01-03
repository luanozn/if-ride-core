package com.ifride.core.driver.controller;

import com.ifride.core.driver.model.dto.VehicleCreationDTO;
import com.ifride.core.driver.model.dto.VehicleResponseDTO;
import com.ifride.core.driver.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/driver/{driverId}/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    public VehicleResponseDTO create(@PathVariable String driverId, @RequestBody VehicleCreationDTO vehicle) {
        return vehicleService.saveBy(vehicle, driverId);
    }

    @GetMapping
    @PreAuthorize("hasRole('DRIVER')")
    public List<VehicleResponseDTO> findByDriverId(@PathVariable String driverId) {
        return vehicleService.getByOwner(driverId);
    }
}
