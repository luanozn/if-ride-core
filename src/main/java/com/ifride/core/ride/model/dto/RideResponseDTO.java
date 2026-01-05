package com.ifride.core.ride.model.dto;

import com.ifride.core.driver.model.dto.DriverSummaryDTO;
import com.ifride.core.driver.model.dto.VehicleResponseDTO;
import com.ifride.core.ride.model.Ride;
import java.math.BigDecimal;
import java.util.List;

public record RideResponseDTO(
        DriverSummaryDTO driver,
        VehicleResponseDTO vehicle,
        String origin,
        String destination,
        List<String> pickupPoints,
        int availableSeats,
        BigDecimal price
) {
    public static RideResponseDTO fromEntity(Ride ride) {
        return new RideResponseDTO(
                DriverSummaryDTO.fromEntity(ride.getDriver()),
                VehicleResponseDTO.fromEntity(ride.getVehicle()),
                ride.getOrigin(),
                ride.getDestination(),
                ride.getPickupPoints(),
                ride.getAvailableSeats(),
                ride.getPrice()
        );
    }
}
