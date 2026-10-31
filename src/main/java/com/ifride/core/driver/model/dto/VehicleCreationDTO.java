package com.ifride.core.driver.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record VehicleCreationDTO(
        @Schema(description = "Modelo do veículo", example = "Toyota Corolla")
        @NotBlank(message = "O modelo do veículo é obrigatório")
        @Size(max = 100, message = "O modelo não pode exceder 100 caracteres")
        String model,

        @Schema(description = "Placa do veículo (formato Mercosul ou antigo)", example = "ABC1D23")
        @NotBlank(message = "A placa é obrigatória")
        @Size(min = 7, max = 8, message = "A placa deve ter entre 7 e 8 caracteres")
        String plate,

        @Schema(description = "Cor predominante", example = "Prata")
        @NotBlank(message = "A cor é obrigatória")
        @Size(max = 50, message = "A cor não pode exceder 50 caracteres")
        String color,

        @Schema(description = "Capacidade total de passageiros (excluindo o motorista)", example = "4")
        @NotNull(message = "A capacidade é obrigatória")
        @Min(value = 1, message = "A capacidade deve ser no mínimo 1 passageiro")
        Integer capacity
) {}