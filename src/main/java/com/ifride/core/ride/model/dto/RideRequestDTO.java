package com.ifride.core.ride.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RideRequestDTO(
        @Schema(description = "ID do veículo que será usado (deve pertencer ao motorista)", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        String vehicleId,

        @Schema(description = "Local de partida", example = "Núcleo de Informática")
        String origin,

        @Schema(description = "Destino final da carona", example = "Terminal Rodoviário de Orizona")
        String destination,

        @Schema(description = "Lista de pontos onde o motorista pode parar no caminho")
        List<String> pickupPoints,

        @Schema(description = "Data e hora da partida (deve ser no futuro)", example = "2026-02-20T14:30:00")
        LocalDateTime departureTime,

        @Schema(description = "Número de vagas disponíveis (não pode exceder a capacidade do veículo)", example = "3")
        Integer availableSeats,

        @Schema(description = "Preço por passageiro (0.00 se for gratuita)", example = "5.50")
        BigDecimal price
) { }