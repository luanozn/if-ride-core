package com.ifride.core.service.ride;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.driver.model.entity.Driver;
import com.ifride.core.driver.model.entity.Vehicle;
import com.ifride.core.driver.model.enums.CnhCategory;
import com.ifride.core.events.models.RideParticipationAcceptedEvent;
import com.ifride.core.ride.model.Ride;
import com.ifride.core.ride.model.RideParticipant;
import com.ifride.core.ride.model.enums.ParticipantStatus;
import com.ifride.core.ride.model.enums.RideStatus;
import com.ifride.core.ride.repository.RideParticipantRepository;
import com.ifride.core.ride.service.RideParticipantService;
import com.ifride.core.ride.service.RideService;
import com.ifride.core.shared.exceptions.api.ConflictException;
import com.ifride.core.shared.exceptions.api.ForbiddenException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class RideParticipantServiceTest {

    @Mock
    private RideParticipantRepository repository;

    @Mock
    private RideService rideService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RideParticipantService service;

    private User driverAuthor;
    private User passenger;
    private Ride ride;

    @BeforeEach
    void setUp() {
        Driver driver = new Driver();
        driver.setId("driver-1");
        driver.setCnhCategory(CnhCategory.AB);
        driver.setCnhExpiration(LocalDate.now().plusDays(1));

        Vehicle vehicle = new Vehicle();
        vehicle.setCapacity(5);
        vehicle.setModel("Ford Fusion 2019");
        vehicle.setPlate("AVP-3045");
        vehicle.setOwner(driver);

        driverAuthor = new User();
        driverAuthor.setId("driver-1");
        driverAuthor.setName("Driver Author");

        driver.setUser(driverAuthor);

        passenger = new User();
        passenger.setId("passenger-1");

        ride = new Ride();
        ride.setId("ride-1");
        ride.setDriver(driver);
        ride.setRideStatus(RideStatus.SCHEDULED);
        ride.setAvailableSeats(3);
        ride.setDepartureTime(LocalDateTime.now().plusHours(2));
        ride.setVehicle(vehicle);
    }

    @Nested
    @DisplayName("Testes de Solicitação de Vaga (requestSeat)")
    class RequestSeatTests {

        @Test
        @DisplayName("Deve solicitar vaga com sucesso")
        void shouldRequestSeatSuccessfully() {
            when(rideService.findById(anyString())).thenReturn(ride);
            when(repository.hasConflict(any(), any(), any())).thenReturn(false);

            assertDoesNotThrow(() -> service.requestSeat(passenger, "ride-1"));
            verify(repository, times(1)).save(any());
        }

        @Test
        @DisplayName("Deve falhar se a carona estiver lotada")
        void shouldThrowConflictWhenRideFull() {
            ride.setAvailableSeats(0);
            when(rideService.findById(anyString())).thenReturn(ride);

            assertThrows(ConflictException.class, () -> service.requestSeat(passenger, "ride-1"));
        }

        @Test
        @DisplayName("Deve falhar se o motorista tentar ser passageiro")
        void shouldThrowForbiddenWhenDriverRequestsOwnRide() {
            when(rideService.findById(anyString())).thenReturn(ride);

            assertThrows(ForbiddenException.class, () -> service.requestSeat(driverAuthor, "ride-1"));
        }
    }

    @Nested
    @DisplayName("Testes de Aceite de Participação (acceptParticipation)")
    class AcceptParticipationTests {

        @Test
        @DisplayName("Deve aceitar participação e disparar evento")
        void shouldAcceptParticipationSuccessfully() {
            RideParticipant participant = new RideParticipant();
            participant.setRide(ride);
            participant.setPassenger(passenger);
            participant.setParticipantStatus(ParticipantStatus.PENDING);

            when(repository.findById(anyString())).thenReturn(Optional.of(participant));
            when(rideService.getCurrentAvailableSeats(anyString())).thenReturn(2);

            service.acceptParticipation("part-1", "driver-1");

            assertEquals(ParticipantStatus.ACCEPTED, participant.getParticipantStatus());
            verify(rideService).decrementAvailableSeats(ride);
            verify(eventPublisher).publishEvent(any(RideParticipationAcceptedEvent.class));
        }

        @Test
        @DisplayName("Deve falhar se o status não for PENDING")
        void shouldThrowConflictWhenStatusNotPending() {
            RideParticipant participant = new RideParticipant();
            participant.setRide(ride);
            participant.setParticipantStatus(ParticipantStatus.REJECTED);

            when(repository.findById(anyString())).thenReturn(Optional.of(participant));

            assertThrows(ConflictException.class, () -> service.acceptParticipation("part-1", "driver-1"));
        }
    }

    @Nested
    @DisplayName("Testes de Cancelamento (cancelParticipation)")
    class CancelParticipationTests {

        @Test
        @DisplayName("Deve cancelar e devolver vaga se já estava aceito")
        void shouldCancelAndReturnSeat() {
            RideParticipant participant = new RideParticipant();
            participant.setRide(ride);
            participant.setPassenger(passenger);
            participant.setParticipantStatus(ParticipantStatus.ACCEPTED);

            when(repository.findById(anyString())).thenReturn(Optional.of(participant));

            service.cancelParticipation("part-1", "passenger-1");

            assertEquals(ParticipantStatus.CANCELLED, participant.getParticipantStatus());
            verify(rideService).incrementAvailableSeats(ride);
        }

        @Test
        @DisplayName("Deve falhar se tentar cancelar após a partida")
        void shouldThrowConflictWhenCancellingAfterDeparture() {
            ride.setDepartureTime(LocalDateTime.now().minusMinutes(10));
            RideParticipant participant = new RideParticipant();
            participant.setRide(ride);
            participant.setPassenger(passenger);

            when(repository.findById(anyString())).thenReturn(Optional.of(participant));

            assertThrows(ConflictException.class, () -> service.cancelParticipation("part-1", "passenger-1"));
        }
    }
}