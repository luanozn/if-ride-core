package com.ifride.core.ride.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RideRequestDTO(
        String vehicleId,
        String origin,
        String destination,
        List<String> pickupPoints,
        LocalDateTime departureTime,
        int availableSeats,
        BigDecimal price
) { }