package com.ifride.core.driver.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record VehicleCreationDTO(
        @Schema(description = "Modelo do veículo", example = "Toyota Corolla")
        String model,

        @Schema(description = "Placa do veículo (formato Mercosul ou antigo)", example = "ABC1D23")
        String plate,

        @Schema(description = "Cor predominante", example = "Prata")
        String color,

        @Schema(description = "Capacidade total de passageiros (excluindo o motorista)", example = "4")
        Integer capacity
) {}