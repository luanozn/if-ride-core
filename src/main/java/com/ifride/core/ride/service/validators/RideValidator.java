package com.ifride.core.ride.service.validators;

import com.ifride.core.driver.model.entity.Driver;
import com.ifride.core.driver.model.entity.Vehicle;
import com.ifride.core.ride.model.dto.RideRequestDTO;
import com.ifride.core.ride.repository.RideRepository;
import com.ifride.core.shared.exceptions.api.ConflictException;
import com.ifride.core.shared.exceptions.api.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RideValidator {

    private final RideRepository rideRepository;

    public void validateRideCreation(Driver driver, Vehicle vehicle, RideRequestDTO rideRequest) {
        checkVehicleBelongsToDriver(vehicle, driver);
        checkDepartureTime(rideRequest);
        checkCapacity(rideRequest, vehicle);
        checkOverlap(driver, rideRequest.departureTime());
    }

    private void checkVehicleBelongsToDriver(Vehicle vehicle, Driver driver) {
        if(!vehicle.getOwner().getId().equals(driver.getId())) {
            throw new ForbiddenException("O veículo %s não pertence ao usuário %s", vehicle.getModel(), driver.getUser().getEmail());
        }
    }

    private void checkOverlap(Driver driver, LocalDateTime newDeparture) {
        LocalDateTime start = newDeparture.minusHours(1);
        LocalDateTime end = newDeparture.plusHours(1);

        if (rideRepository.existsOverlap(driver.getId(), start, end)) {
            throw new ConflictException("Conflito de Horário! Você já possui uma carona agendada próxima a este horário.");
        }
    }

    private void checkDepartureTime(RideRequestDTO rideRequest) {
        if (rideRequest.departureTime().isBefore(LocalDateTime.now())) {
            throw new ForbiddenException("A data de partida não pode ser no passado.");
        }
    }

    private void checkCapacity(RideRequestDTO rideRequest, Vehicle vehicle) {
        if (rideRequest.availableSeats() > vehicle.getCapacity()) {
            throw new ConflictException("O número de vagas excede a capacidade do veículo (%d).", vehicle.getCapacity());
        }
    }
}
