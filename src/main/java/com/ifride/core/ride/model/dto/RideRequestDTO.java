package com.ifride.core.ride.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ifride.core.ride.model.Ride;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RideRequestDTO(
        @Schema(description = "ID do veículo que será usado (deve pertencer ao motorista)", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        String vehicleId,

        @Schema(description = "Local de partida", example = "Núcleo de Informática")
        String origin,

        @Schema(description = "Destino final da carona", example = "Terminal Rodoviário de Orizona")
        String destination,

        @Schema(description = "Lista de pontos onde o motorista pode parar no caminho", example = "[\"Supermercado União\", \"Hotel Carvalho\", \"Trevo\"]")
        List<String> pickupPoints,

        @Schema(description = "Data e hora da partida (deve ser no futuro) (Opcional se a carona for recorrente)", example = "2026-02-20T14:30:00")
        LocalDateTime departureTime,

        @Schema(description = "Número de vagas disponíveis (não pode exceder a capacidade do veículo)", example = "3")
        Integer availableSeats,

        @Schema(description = "Preço por passageiro (0.00 se for gratuita)", example = "5.50")
        BigDecimal price,

        @Schema(description = "Cadastra uma carona recorrente (Que se repete toda semana) - Não necessário se a carona não for recorrente", defaultValue = "false")
        boolean isRecurrent,

        @Schema(description = "Dia da semana que essa carona ocorrerá - É obrigatório caso isRecurrent seja verdadeiro", example = "MONDAY")
        DayOfWeek recurrentDay,

        @Schema(description = "Horário em que a carona recorrente acontecerá - É obrigatório caso isRecurrent seja verdadeiro", example = "14:30:00")
        @JsonFormat(pattern = "HH:mm:ss")
        LocalTime recurrencyDeparture
) {
        public static RideRequestDTO fromRideForRecurrency(Ride ride) {
                return new RideRequestDTO(
                        ride.getVehicle().getId(),
                        ride.getOrigin(),
                        ride.getDestination(),
                        ride.getPickupPoints(),
                        ride.getDepartureTime().plusWeeks(1),
                        ride.getAvailableSeats(),
                        ride.getPrice(),
                        ride.isRecurrent(),
                        ride.getRecurrentDay(),
                        ride.getRecurrencyDeparture()
                );
        }
}