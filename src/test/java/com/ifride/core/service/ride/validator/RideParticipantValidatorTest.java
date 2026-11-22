package com.ifride.core.service.ride.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.driver.model.entity.Driver;
import com.ifride.core.ride.model.Ride;
import com.ifride.core.ride.model.RideParticipant;
import com.ifride.core.ride.model.enums.ParticipantStatus;
import com.ifride.core.ride.model.enums.RideStatus;
import com.ifride.core.ride.repository.RideParticipantRepository;
import com.ifride.core.ride.service.validators.RideParticipantValidator;
import com.ifride.core.shared.exceptions.api.BadRequestException;
import com.ifride.core.shared.exceptions.api.ConflictException;
import com.ifride.core.shared.exceptions.api.ForbiddenException;
import com.ifride.core.shared.exceptions.api.PreconditionFailedException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RideParticipantValidatorTest {

    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");

    @Mock private RideParticipantRepository repository;

    private RideParticipantValidator validator;
    private User passenger;
    private Ride ride;
    private RideParticipant participant;

    @BeforeEach
    void setUp() {
        validator = new RideParticipantValidator(repository, Clock.fixed(NOW, ZoneOffset.UTC));

        passenger = new User();
        passenger.setId("passenger-1");
        User driverUser = new User();
        driverUser.setId("driver-1");
        Driver driver = new Driver();
        driver.setUser(driverUser);
        driver.setId("driver-1");

        ride = new Ride();
        ride.setId("ride-1");
        ride.setDriver(driver);
        ride.setRideStatus(RideStatus.SCHEDULED);
        ride.setAvailableSeats(2);
        ride.setDepartureTime(NOW.plusSeconds(7200));
        ride.setPickupPoints(List.of("Ponto A", "Ponto B"));

        participant = new RideParticipant();
        participant.setRide(ride);
        participant.setPassenger(passenger);
        participant.setParticipantStatus(ParticipantStatus.PENDING);
    }

    @Nested
    @DisplayName("Solicitação de vaga")
    class RequestTests {
        @Test
        @DisplayName("Aceita uma solicitação válida")
        void acceptsValidRequest() {
            when(repository.existsByRideIdAndPassengerIdAndParticipantStatusIn(any(), any(), anyList())).thenReturn(false);
            when(repository.hasConflict(any(), any(), any())).thenReturn(false);

            assertDoesNotThrow(() -> validator.validateRequest(passenger, ride, "Ponto A"));
            verify(repository).existsByRideIdAndPassengerIdAndParticipantStatusIn(
                    "ride-1", "passenger-1", List.of(ParticipantStatus.PENDING, ParticipantStatus.ACCEPTED));
        }

        @Test
        @DisplayName("Rejeita solicitação ativa duplicada, carona indisponível ou sem vagas")
        void rejectsDuplicateUnavailableOrFullRide() {
            when(repository.existsByRideIdAndPassengerIdAndParticipantStatusIn(any(), any(), anyList())).thenReturn(true);
            assertThrows(ConflictException.class, () -> validator.validateRequest(passenger, ride, "Ponto A"));

            when(repository.existsByRideIdAndPassengerIdAndParticipantStatusIn(any(), any(), anyList())).thenReturn(false);
            ride.setRideStatus(RideStatus.FULL);
            assertThrows(ConflictException.class, () -> validator.validateRequest(passenger, ride, "Ponto A"));

            ride.setRideStatus(RideStatus.SCHEDULED);
            ride.setAvailableSeats(0);
            assertThrows(ConflictException.class, () -> validator.validateRequest(passenger, ride, "Ponto A"));
            verify(repository, never()).hasConflict(any(), any(), any());
        }

        @Test
        @DisplayName("Rejeita conflito de agenda, embarque inválido, próprio motorista e horário expirado")
        void rejectsOtherInvalidRequests() {
            when(repository.existsByRideIdAndPassengerIdAndParticipantStatusIn(any(), any(), anyList())).thenReturn(false);
            when(repository.hasConflict(any(), any(), any())).thenReturn(true);
            assertThrows(ConflictException.class, () -> validator.validateRequest(passenger, ride, "Ponto A"));

            when(repository.hasConflict(any(), any(), any())).thenReturn(false);
            assertThrows(BadRequestException.class, () -> validator.validateRequest(passenger, ride, "Outro"));

            User driver = new User();
            driver.setId("driver-1");
            assertThrows(ForbiddenException.class, () -> validator.validateRequest(driver, ride, "Ponto A"));

            ride.setDepartureTime(NOW.minusSeconds(1));
            assertThrows(ConflictException.class, () -> validator.validateRequest(passenger, ride, "Ponto A"));
        }

        @Test
        @DisplayName("Permite qualquer ponto quando a carona não define pontos de embarque")
        void allowsRequestWithoutConfiguredPickupPoints() {
            ride.setPickupPoints(List.of());
            when(repository.existsByRideIdAndPassengerIdAndParticipantStatusIn(any(), any(), anyList())).thenReturn(false);
            when(repository.hasConflict(any(), any(), any())).thenReturn(false);

            assertDoesNotThrow(() -> validator.validateRequest(passenger, ride, "Ponto livre"));
        }
    }

    @Nested
    @DisplayName("Aceite e rejeição")
    class ProcessingTests {
        @Test
        @DisplayName("Aceita validar solicitação pendente pelo motorista da carona")
        void acceptsDriverProcessingPendingRequest() {
            assertDoesNotThrow(() -> validator.validateAcceptance(participant, "driver-1"));
            assertDoesNotThrow(() -> validator.validateRejection(participant, "driver-1"));
        }

        @Test
        @DisplayName("Impede ação de outro motorista, solicitação processada e aceite após partida")
        void rejectsUnauthorizedProcessedOrExpiredAcceptance() {
            assertThrows(ForbiddenException.class, () -> validator.validateAcceptance(participant, "other-driver"));
            assertThrows(ForbiddenException.class, () -> validator.validateRejection(participant, "other-driver"));

            participant.setParticipantStatus(ParticipantStatus.ACCEPTED);
            assertThrows(ConflictException.class, () -> validator.validateAcceptance(participant, "driver-1"));
            assertThrows(ConflictException.class, () -> validator.validateRejection(participant, "driver-1"));

            participant.setParticipantStatus(ParticipantStatus.PENDING);
            ride.setDepartureTime(NOW.minusSeconds(1));
            assertThrows(ConflictException.class, () -> validator.validateAcceptance(participant, "driver-1"));
        }

        @Test
        @DisplayName("Só permite aceitar em carona agendada")
        void rejectsAcceptanceForNonScheduledRide() {
            ride.setRideStatus(RideStatus.FULL);
            assertThrows(ConflictException.class, () -> validator.validateAcceptance(participant, "driver-1"));
        }
    }

    @Nested
    @DisplayName("Cancelamento e exclusão de usuário")
    class CancellationAndDeletionTests {
        @Test
        @DisplayName("Permite cancelamento do passageiro antes da partida")
        void permitsPassengerCancellation() {
            assertDoesNotThrow(() -> validator.validateCancelling(participant, "passenger-1"));
        }

        @Test
        @DisplayName("Impede cancelar participação alheia, processada ou depois da partida")
        void rejectsInvalidCancellation() {
            assertThrows(ForbiddenException.class, () -> validator.validateCancelling(participant, "other-passenger"));

            participant.setParticipantStatus(ParticipantStatus.REJECTED);
            assertThrows(ConflictException.class, () -> validator.validateCancelling(participant, "passenger-1"));
            participant.setParticipantStatus(ParticipantStatus.CANCELLED);
            assertThrows(ConflictException.class, () -> validator.validateCancelling(participant, "passenger-1"));

            participant.setParticipantStatus(ParticipantStatus.ACCEPTED);
            ride.setDepartureTime(NOW.minusSeconds(1));
            assertThrows(ConflictException.class, () -> validator.validateCancelling(participant, "passenger-1"));
        }

        @Test
        @DisplayName("Impede cancelamento quando a carona está em andamento, finalizada ou cancelada")
        void rejectsCancellationForClosedRideStatuses() {
            for (RideStatus status : List.of(RideStatus.IN_PROGRESS, RideStatus.FINISHED, RideStatus.CANCELLED)) {
                ride.setRideStatus(status);
                assertThrows(ConflictException.class, () -> validator.validateCancelling(participant, "passenger-1"), status.name());
            }
        }

        @Test
        @DisplayName("Impede exclusão da conta enquanto há caronas futuras válidas")
        void rejectsDeletionWithUpcomingRides() {
            when(repository.hasValidUpcomingRides(passenger, NOW)).thenReturn(true);
            assertThrows(PreconditionFailedException.class, () -> validator.validateUserDeletion(passenger));
            verify(repository).hasValidUpcomingRides(passenger, NOW);
        }

        @Test
        @DisplayName("Permite exclusão da conta sem caronas futuras válidas")
        void allowsDeletionWithoutUpcomingRides() {
            when(repository.hasValidUpcomingRides(passenger, NOW)).thenReturn(false);
            assertDoesNotThrow(() -> validator.validateUserDeletion(passenger));
        }
    }
}
