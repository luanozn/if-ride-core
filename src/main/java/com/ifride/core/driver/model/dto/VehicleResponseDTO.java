package com.ifride.core.driver.model.dto;

import com.ifride.core.driver.model.entity.Vehicle;
import io.swagger.v3.oas.annotations.media.Schema;

public record VehicleResponseDTO(
        @Schema(description = "ID único do veículo (UUID)")
        String id,

        @Schema(description = "Modelo do veículo")
        String model,

        @Schema(description = "Placa cadastrada")
        String plate,

        @Schema(description = "Cor do veículo")
        String color,

        @Schema(description = "Resumo dos dados do proprietário")
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
