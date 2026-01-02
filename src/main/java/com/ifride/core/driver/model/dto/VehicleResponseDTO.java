package com.ifride.core.driver.model.dto;

import com.ifride.core.driver.model.entity.Vehicle;

public record VehicleResponseDTO(
        String id,
        String model,
        String plate,
        String color,
        DriverSummaryDTO owner
) {
    public static VehicleResponseDTO fromEntity(Vehicle vehicle) {
        return new VehicleResponseDTO(
                vehicle.getId(),
                vehicle.getModel(),
                vehicle.getPlate(),
                vehicle.getColor(),
                DriverSummaryDTO.fromEntity(vehicle.getOwner())
        );
    }
}
