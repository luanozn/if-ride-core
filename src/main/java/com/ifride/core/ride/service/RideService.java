package com.ifride.core.ride.service;

import static com.ifride.core.shared.utils.DateTimeUtils.getDepartureTime;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.driver.service.DriverService;
import com.ifride.core.driver.service.VehicleService;
import com.ifride.core.events.models.RideFinishedEvent;
import com.ifride.core.ride.model.Ride;
import com.ifride.core.ride.model.dto.RideRequestDTO;
import com.ifride.core.ride.model.dto.RideResponseDTO;
import com.ifride.core.ride.model.enums.RideStatus;
import com.ifride.core.ride.repository.RideRepository;
import com.ifride.core.ride.service.validators.RideValidator;
import com.ifride.core.shared.exceptions.api.ConflictException;
import com.ifride.core.shared.exceptions.api.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RideService {

    private final RideRepository rideRepository;
    private final DriverService driverService;
    private final VehicleService vehicleService;
    private final RideValidator rideValidator;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public RideResponseDTO createRide(String driverId, RideRequestDTO rideRequestDTO) {
        BigDecimal finalPrice = Objects.requireNonNullElse(rideRequestDTO.price(), BigDecimal.ZERO);

        var ride = new Ride();

        var driver = driverService.findById(driverId);
        var vehicle = vehicleService.findById(rideRequestDTO.vehicleId());
        rideValidator.validateRideCreation(driver, vehicle, rideRequestDTO);

        ride.setDriver(driver);
        ride.setVehicle(vehicle);
        ride.setOrigin(rideRequestDTO.origin());
        ride.setDestination(rideRequestDTO.destination());
        ride.setAvailableSeats(rideRequestDTO.availableSeats());
        ride.setTotalSeats(rideRequestDTO.availableSeats());
        ride.setPickupPoints(rideRequestDTO.pickupPoints());
        ride.setDepartureTime(getDepartureTime(rideRequestDTO));
        ride.setRecurrent(rideRequestDTO.isRecurrent());
        ride.setRecurrentDay(rideRequestDTO.recurrentDay());
        ride.setRecurrencyDeparture(rideRequestDTO.recurrencyDeparture());

        if(finalPrice.compareTo(BigDecimal.ZERO) > 0) {
            ride.setPrice(rideRequestDTO.price());
        }

        ride = rideRepository.save(ride);

        return RideResponseDTO.fromEntity(ride);
    }

    @Transactional
    public void decrementAvailableSeats(Ride ride) {
        int rowsUpdated = rideRepository.decrementAvailableSeats(ride.getId());

        if (rowsUpdated == 0) {
            throw new ConflictException("Não foi possível reservar a vaga: Carona já está lotada.");
        }
    }

    @Transactional(readOnly=true)
    public Ride findById(String id) {
        return rideRepository.findById(id).orElseThrow(() -> new NotFoundException("Carona com o ID %s não encontrada!", id));
    }

    @Transactional(readOnly=true)
    public Integer getCurrentAvailableSeats(String rideId) {
        return rideRepository.getCurrentAvailableSeats(rideId);
    }

    @Transactional(readOnly = true)
    public Page<RideResponseDTO> findAvailableRides(String origin, String destination, boolean includeFull, Pageable pageable) {
        return rideRepository.findAvailableRides(origin, destination, includeFull, pageable)
                .map(RideResponseDTO::fromEntity);
    }

    @Transactional
    public void updateStatus(String rideId, RideStatus newStatus) {
        rideRepository.updateStatus(rideId, newStatus);
    }

    @Transactional
    public void startRide(String rideId, User requester) {
        Ride ride = findById(rideId);
        rideValidator.validateRideUpdate(ride, requester,RideStatus.IN_PROGRESS);

        updateStatus(rideId, RideStatus.IN_PROGRESS);
    }

    @Transactional
    public void finishRide(String rideId, User requester) {
        Ride ride = findById(rideId);
        rideValidator.validateRideUpdate(ride, requester,RideStatus.FINISHED);

        updateStatus(rideId, RideStatus.FINISHED);
        ride = findById(rideId);
        eventPublisher.publishEvent(new RideFinishedEvent(ride));
    }

    @Transactional
    public void incrementAvailableSeats(Ride ride) {
        int rowsUpdated = rideRepository.incrementAvailableSeats(ride.getId());
        if (rowsUpdated == 0) {
            throw new ConflictException("Não foi possível devolver a vaga: Limite do veículo atingido.");
        }
    }
}
