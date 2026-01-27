package com.ifride.core.service.ride;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.driver.model.entity.Driver;
import com.ifride.core.driver.model.entity.Vehicle;
import com.ifride.core.driver.model.enums.CnhCategory;
import com.ifride.core.events.models.RideParticipationAcceptedEvent;
import com.ifride.core.events.models.RideParticipationCancelledEvent;
import com.ifride.core.events.models.RideParticipationRejectedEvent;
import com.ifride.core.ride.model.Ride;
import com.ifride.core.ride.model.RideParticipant;
import com.ifride.core.ride.model.dto.RideParticipantRequestDTO;
import com.ifride.core.ride.model.enums.ParticipantStatus;
import com.ifride.core.ride.model.enums.RideStatus;
import com.ifride.core.ride.repository.RideParticipantRepository;
import com.ifride.core.ride.service.RideParticipantService;
import com.ifride.core.ride.service.RideService;
import com.ifride.core.ride.service.validators.RideParticipantValidator;
import com.ifride.core.shared.exceptions.api.ConflictException;
import com.ifride.core.shared.exceptions.api.NotFoundException;
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
    private RideParticipantValidator rideValidator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RideParticipantService service;

    private User passenger;
    private Ride ride;
    private RideParticipant participant;

    @BeforeEach
    void setUp() {
        passenger = new User();
        passenger.setId("passenger-1");

        User driverUser = new User();
        driverUser.setId("driver-1");


        Driver driver = new Driver();
        driver.setUser(driverUser);
        driver.setCnhCategory(CnhCategory.B);

        Vehicle vehicle = new Vehicle();
        vehicle.setCapacity(5);
        vehicle.setModel("Ford Fusion 2019");
        vehicle.setPlate("AVP-3045");
        vehicle.setOwner(driver);

        ride = new Ride();
        ride.setId("ride-1");
        ride.setDriver(driver);
        ride.setRideStatus(RideStatus.SCHEDULED);
        ride.setVehicle(vehicle);

        participant = new RideParticipant();
        participant.setId("part-1");
        participant.setPassenger(passenger);
        participant.setRide(ride);
        participant.setParticipantStatus(ParticipantStatus.PENDING);
    }

    @Nested
    @DisplayName("Solicitação de Vaga (requestSeat)")
    class RequestSeatTests {

        @Test
        @DisplayName("Deve solicitar vaga com sucesso e salvar")
        void shouldRequestSeatSuccessfully() {
            when(rideService.findById("ride-1")).thenReturn(ride);
            doNothing().when(rideValidator).validateRequest(any(), any(), any());

            var response = service.requestSeat(passenger, "ride-1", new RideParticipantRequestDTO("Ponto A"));

            assertNotNull(response);
            verify(repository).save(any(RideParticipant.class));
            verify(rideValidator).validateRequest(passenger, ride, "Ponto A");
        }

        @Test
        @DisplayName("Deve repassar exceção do validador")
        void shouldPropagateValidatorException() {
            when(rideService.findById("ride-1")).thenReturn(ride);
            doThrow(new ConflictException("Erro de validação"))
                    .when(rideValidator).validateRequest(any(), any(), any());

            assertThrows(ConflictException.class, () ->
                    service.requestSeat(passenger, "ride-1", new RideParticipantRequestDTO("Ponto A")));

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Aceite de Participação (acceptParticipation)")
    class AcceptParticipationTests {

        @Test
        @DisplayName("Deve aceitar participação e manter status SCHEDULED se houver vagas")
        void shouldAcceptSuccessfullyWithSeatsRemaining() {
            when(repository.findById("part-1")).thenReturn(Optional.of(participant));
            when(rideService.getCurrentAvailableSeats("ride-1")).thenReturn(1);

            service.acceptParticipation("part-1", "driver-1");

            assertEquals(ParticipantStatus.ACCEPTED, participant.getParticipantStatus());
            verify(rideService).decrementAvailableSeats(ride);
            verify(rideService, never()).updateStatus(anyString(), any());
            verify(eventPublisher).publishEvent(any(RideParticipationAcceptedEvent.class));
        }

        @Test
        @DisplayName("Deve aceitar e atualizar para FULL quando as vagas acabarem")
        void shouldAcceptAndUpdateToFullWhenNoSeatsLeft() {
            when(repository.findById("part-1")).thenReturn(Optional.of(participant));
            when(rideService.getCurrentAvailableSeats("ride-1")).thenReturn(0);

            service.acceptParticipation("part-1", "driver-1");

            verify(rideService).updateStatus("ride-1", RideStatus.FULL);
            verify(repository).save(participant);
        }
    }

    @Nested
    @DisplayName("Rejeição de Participação (rejectParticipation)")
    class RejectParticipationTests {

        @Test
        @DisplayName("Deve rejeitar participação com sucesso")
        void shouldRejectSuccessfully() {
            when(repository.findById("part-1")).thenReturn(Optional.of(participant));

            service.rejectParticipation("part-1", "driver-1");

            assertEquals(ParticipantStatus.REJECTED, participant.getParticipantStatus());
            verify(rideValidator).validateRejection(participant, "driver-1");
            verify(eventPublisher).publishEvent(any(RideParticipationRejectedEvent.class));
        }
    }

    @Nested
    @DisplayName("Cancelamento (cancelParticipation)")
    class CancelParticipationTests {

        @Test
        @DisplayName("Deve cancelar e devolver vaga se estava aceito")
        void shouldCancelAndReturnSeatIfAccepted() {
            participant.setParticipantStatus(ParticipantStatus.ACCEPTED);
            when(repository.findById("part-1")).thenReturn(Optional.of(participant));

            service.cancelParticipation("part-1", "passenger-1");

            assertEquals(ParticipantStatus.CANCELLED, participant.getParticipantStatus());
            verify(rideService).incrementAvailableSeats(ride);
            verify(eventPublisher).publishEvent(any(RideParticipationCancelledEvent.class));
        }

        @Test
        @DisplayName("Deve cancelar e voltar para SCHEDULED se carona estava FULL")
        void shouldRevertStatusToScheduledIfWasFull() {
            participant.setParticipantStatus(ParticipantStatus.ACCEPTED);
            ride.setRideStatus(RideStatus.FULL);
            when(repository.findById("part-1")).thenReturn(Optional.of(participant));

            service.cancelParticipation("part-1", "passenger-1");

            verify(rideService).updateStatus("ride-1", RideStatus.SCHEDULED);
        }

        @Test
        @DisplayName("Não deve alterar vagas se status era PENDING")
        void shouldNotChangeSeatsIfStatusWasPending() {
            participant.setParticipantStatus(ParticipantStatus.PENDING);
            when(repository.findById("part-1")).thenReturn(Optional.of(participant));

            service.cancelParticipation("part-1", "passenger-1");

            verify(rideService, never()).incrementAvailableSeats(any());
        }
    }

    @Nested
    @DisplayName("Busca por ID (getById)")
    class GetByIdTests {
        @Test
        @DisplayName("Deve lançar NotFoundException quando ID não existir")
        void shouldThrowNotFound() {
            when(repository.findById("invalido")).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.acceptParticipation("invalido", "driver-1"));
        }
    }
}