package com.ifride.core.ride.model.dto;

import com.ifride.core.driver.model.dto.DriverSummaryDTO;
import com.ifride.core.driver.model.dto.VehicleResponseDTO;
import com.ifride.core.ride.model.Ride;
import com.ifride.core.ride.model.enums.RideStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RideResponseDTO(
        @Schema(description = "ID da carona")
        String id,

        @Schema(description = "Dados resumidos do motorista")
        DriverSummaryDTO driver,

        @Schema(description = "Dados do veículo utilizado na carona")
        VehicleResponseDTO vehicle,

        @Schema(description = "Local de partida", example = "Campus Central")
        String origin,

        @Schema(description = "Destino final", example = "Estação de Metrô")
        String destination,

        @Schema(description = "Lista de pontos de parada intermediários")
        List<String> pickupPoints,

        @Schema(description = "Número atual de vagas livres", example = "2")
        int availableSeats,

        @Schema(description = "Preço da carona", example = "5.00")
        BigDecimal price,

        @Schema(description = "Data e hora de partida")
        LocalDateTime departureTime,

        @Schema(description = "Status da carona")
        RideStatus rideStatus
) {
    public static RideResponseDTO fromEntity(Ride ride) {
        return new RideResponseDTO(
                ride.getId(),
                DriverSummaryDTO.fromEntity(ride.getDriver()),
                VehicleResponseDTO.fromEntity(ride.getVehicle()),
                ride.getOrigin(),
                ride.getDestination(),
                ride.getPickupPoints(),
                ride.getAvailableSeats(),
                ride.getPrice(),
                ride.getDepartureTime(),
                ride.getRideStatus()
        );
    }
}
