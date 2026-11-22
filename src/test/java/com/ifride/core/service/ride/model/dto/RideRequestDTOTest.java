package com.ifride.core.service.ride.model.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ifride.core.driver.model.entity.Vehicle;
import com.ifride.core.ride.model.Ride;
import com.ifride.core.ride.model.dto.RideRequestDTO;
import com.ifride.core.ride.model.enums.RideStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RideRequestDTOTest {

    @Test
    @DisplayName("Monta a recorrência semanal com todas as vagas originais, não apenas as livres")
    void recurrenceUsesOriginalSeatCountAndAdvancesDepartureByOneWeek() {
        Instant departure = Instant.parse("2026-11-23T12:00:00Z");
        Vehicle vehicle = new Vehicle();
        vehicle.setId("vehicle-1");

        Ride ride = new Ride();
        ride.setVehicle(vehicle);
        ride.setOrigin("Origem");
        ride.setDestination("Destino");
        ride.setPickupPoints(List.of("Ponto A"));
        ride.setDepartureTime(departure);
        ride.setTotalSeats(4);
        ride.setAvailableSeats(1);
        ride.setPrice(new BigDecimal("12.50"));
        ride.setRecurrent(true);
        ride.setRideStatus(RideStatus.FULL);

        RideRequestDTO recurrence = RideRequestDTO.fromRideForRecurrency(ride);

        assertEquals("vehicle-1", recurrence.vehicleId());
        assertEquals(departure.plus(7, ChronoUnit.DAYS), recurrence.departureTime());
        assertEquals(4, recurrence.availableSeats());
        assertEquals(ride.getPickupPoints(), recurrence.pickupPoints());
        assertEquals(ride.getPrice(), recurrence.price());
    }
}
