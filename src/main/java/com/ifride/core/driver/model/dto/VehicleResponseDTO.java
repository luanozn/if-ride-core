package com.ifride.core.driver.model.dto;

import com.ifride.core.driver.model.entity.Vehicle;
import io.swagger.v3.oas.annotations.media.Schema;

public record VehicleResponseDTO(
        @Schema(description = "ID único do veículo (UUID)", example = "3bbe073f-4f56-4b61-88c5-24a3eab6620e")
        String id,

        @Schema(description = "Modelo do veículo", example = "Fiat Argo 2017")
        String model,

        @Schema(description = "Placa cadastrada", example = "ABC-1234")
        String plate,

        @Schema(description = "Cor do veículo", example = "Vermelho")
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
