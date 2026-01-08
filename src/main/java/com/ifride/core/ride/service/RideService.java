package com.ifride.core.ride.service;

import com.ifride.core.driver.service.DriverService;
import com.ifride.core.driver.service.VehicleService;
import com.ifride.core.ride.model.Ride;
import com.ifride.core.ride.model.dto.RideRequestDTO;
import com.ifride.core.ride.model.dto.RideResponseDTO;
import com.ifride.core.ride.repository.RideRepository;
import com.ifride.core.shared.exceptions.api.ConflictException;
import com.ifride.core.shared.exceptions.api.ForbiddenException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RideService {

    private final RideRepository rideRepository;
    private final DriverService driverService;
    private final VehicleService vehicleService;

    public RideResponseDTO createRide(String driverId, RideRequestDTO rideRequestDTO) {
        BigDecimal finalPrice = Objects.requireNonNullElse(rideRequestDTO.price(), BigDecimal.ZERO);

        var ride = new Ride();

        var driver = driverService.findById(driverId);
        var vehicle = vehicleService.findById(rideRequestDTO.vehicleId());

        if(!vehicle.getOwner().getId().equals(driverId)) {
            throw new ForbiddenException("O veículo %s não pertence ao usuário %s", vehicle.getModel(), driver.getUser().getEmail());
        }
        validateNoOverlap(driverId, rideRequestDTO.departureTime());

        if (rideRequestDTO.departureTime().isBefore(LocalDateTime.now())) {
            throw new ForbiddenException("A data de partida não pode ser no passado.");
        }

        if (rideRequestDTO.availableSeats() > vehicle.getCapacity()) {
            throw new ConflictException("O número de vagas excede a capacidade do veículo (%d).", vehicle.getCapacity());
        }

        ride.setDriver(driver);
        ride.setVehicle(vehicle);
        ride.setOrigin(rideRequestDTO.origin());
        ride.setDestination(rideRequestDTO.destination());
        ride.setAvailableSeats(rideRequestDTO.availableSeats());
        ride.setTotalSeats(rideRequestDTO.availableSeats());
        ride.setDepartureTime(rideRequestDTO.departureTime());

        ride.setPickupPoints(
            rideRequestDTO.pickupPoints()
                    .stream()
                    .map(this::normalize)
                    .toList()
        );

        if(finalPrice.compareTo(BigDecimal.ZERO) > 0) {
            ride.setPrice(rideRequestDTO.price());
        }

        ride = rideRepository.save(ride);

        return RideResponseDTO.fromEntity(ride);
    }

    private void validateNoOverlap(String driverId, LocalDateTime newDeparture) {
        LocalDateTime start = newDeparture.minusHours(1);
        LocalDateTime end = newDeparture.plusHours(1);

        if (rideRepository.existsOverlap(driverId, start, end)) {
            throw new ConflictException("Conflito de Horário! Você já possui uma carona agendada próxima a este horário.");
        }
    }

    private String normalize(String input) {
        if (input == null) return null;
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").toLowerCase().trim();
    }
}
