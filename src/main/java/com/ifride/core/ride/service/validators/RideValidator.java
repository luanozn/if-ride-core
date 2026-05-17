package com.ifride.core.ride.service.validators;

import static com.ifride.core.shared.utils.DateTimeUtils.getDepartureTime;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.driver.model.entity.Driver;
import com.ifride.core.driver.model.entity.Vehicle;
import com.ifride.core.driver.service.DriverService;
import com.ifride.core.ride.model.Ride;
import com.ifride.core.ride.model.dto.RideRequestDTO;
import com.ifride.core.ride.model.enums.RideStatus;
import com.ifride.core.ride.repository.RideRepository;
import com.ifride.core.shared.exceptions.api.BadRequestException;
import com.ifride.core.shared.exceptions.api.ConflictException;
import com.ifride.core.shared.exceptions.api.ForbiddenException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RideValidator {

    private final RideRepository rideRepository;

    public void validateRideUpdate(Ride ride, User requester, RideStatus newStatus) {
        checkOwnership(ride, requester);
        checkStatus(ride, newStatus);
        checkRideStartUnicity(ride);
        checkMinimumDepartureTime(ride);
    }

    public void validateRideCreation(Driver driver, Vehicle vehicle, RideRequestDTO rideRequest) {
        checkRecurrency(rideRequest);
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
        LocalDateTime currentDepartureTime = getDepartureTime(rideRequest);
        if (currentDepartureTime.isBefore(LocalDateTime.now())) {
            throw new ForbiddenException("A data de partida não pode ser no passado.");
        }
    }

    private void checkCapacity(RideRequestDTO rideRequest, Vehicle vehicle) {
        if (rideRequest.availableSeats() > vehicle.getCapacity()) {
            throw new ConflictException("O número de vagas excede a capacidade do veículo (%d).", vehicle.getCapacity());
        }
    }

    private void checkRecurrency(RideRequestDTO rideRequest) {
        if(rideRequest.isRecurrent() && Objects.isNull(rideRequest.recurrentDay())) {
            throw new BadRequestException("O dia da semana é obrigatório quando a carona é recorrente!");
        }

        if(rideRequest.isRecurrent() && Objects.isNull(rideRequest.recurrencyDeparture())) {
            throw new BadRequestException("O horário de partida é obrigatório quando a carona é recorrente!");
        }

        if(!rideRequest.isRecurrent() && Objects.isNull(rideRequest.departureTime())) {
            throw new BadRequestException("A data de partida é obrigatória se a carona não é recorrente!");
        }
    }

    private void checkOwnership(Ride ride, User requester) {
        if(!ride.getDriver().getId().equals(requester.getId())) {
            throw new BadRequestException("Somente o motorista da carona pode iniciá-la!");
        }
    }

    private void checkStatus(Ride ride, RideStatus newStatus) {
        if(ride.getRideStatus().equals(newStatus)) {
            throw new BadRequestException("A carona já está no status requisitado!");
        }

        if(newStatus.equals(RideStatus.FINISHED) && !ride.getRideStatus().equals(RideStatus.IN_PROGRESS)) {
            throw new BadRequestException("Não é possível finalizar uma carona não iniciada!");
        }

        if(ride.getRideStatus().equals(RideStatus.FINISHED)) {
            throw new BadRequestException("Não é possível alterar o status de uma carona FINALIZADA!");
        }
    }

    private void checkRideStartUnicity(Ride ride) {
        boolean driverHasRidesInProgress = rideRepository.existsByDriverIdAndRideStatus(ride.getDriver().getId(), RideStatus.IN_PROGRESS);

        if(driverHasRidesInProgress) {
            throw new ConflictException("O motorista já possui uma carona em andamento!");
        }
    }

    private void checkMinimumDepartureTime(Ride ride) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime allowedStartTime = ride.getDepartureTime().minusMinutes(30);

        if (now.isBefore(allowedStartTime)) {
            throw new ConflictException("A carona só pode ser iniciada 30 minutos antes da partida.");
        }
    }
}
