package com.ifride.core.service.ride.validator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RideParticipantValidatorTest {

    @Mock
    private RideParticipantRepository repository;

    @InjectMocks
    private RideParticipantValidator validator;

    private User author;
    private Ride ride;
    private RideParticipant participant;

    @BeforeEach
    void setUp() {
        author = new User();
        author.setId("user-1");

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
        ride.setDepartureTime(LocalDateTime.now().plusHours(2));
        ride.setPickupPoints(List.of("Trevo", "Hotel"));

        participant = new RideParticipant();
        participant.setRide(ride);
        participant.setPassenger(author);
        participant.setParticipantStatus(ParticipantStatus.PENDING);
    }

    @Nested
    @DisplayName("Validação de Solicitação (validateRequest)")
    class ValidateRequestTests {

        @Test
        @DisplayName("Deve passar se todos os critérios forem válidos")
        void shouldPassWhenRequestIsValid() {
            when(repository.existsByRideIdAndPassengerIdAndParticipantStatusIn(any(), any(), any())).thenReturn(false);
            when(repository.hasConflict(any(), any(), any())).thenReturn(false);

            assertDoesNotThrow(() -> validator.validateRequest(author, ride, "Trevo"));
        }

        @Test
        @DisplayName("Deve falhar se já houver solicitação ativa")
        void shouldThrowConflictWhenAlreadyRequested() {
            when(repository.existsByRideIdAndPassengerIdAndParticipantStatusIn(any(), any(), any())).thenReturn(true);
            assertThrows(ConflictException.class, () -> validator.validateRequest(author, ride, "Trevo"));
        }

        @Test
        @DisplayName("Deve falhar se a carona não estiver SCHEDULED")
        void shouldThrowConflictWhenRideNotScheduled() {
            ride.setRideStatus(RideStatus.FULL);
            assertThrows(ConflictException.class, () -> validator.validateRequest(author, ride, "Trevo"));
        }

        @Test
        @DisplayName("Deve falhar se não houver vagas")
        void shouldThrowConflictWhenNoSeats() {
            ride.setAvailableSeats(0);
            assertThrows(ConflictException.class, () -> validator.validateRequest(author, ride, "Trevo"));
        }

        @Test
        @DisplayName("Deve falhar em caso de conflito de horário")
        void shouldThrowConflictOnTimeOverlap() {
            when(repository.hasConflict(any(), any(), any())).thenReturn(true);
            assertThrows(ConflictException.class, () -> validator.validateRequest(author, ride, "Trevo"));
        }

        @Test
        @DisplayName("Deve falhar se o motorista tentar ser passageiro")
        void shouldThrowForbiddenWhenOwnerRequests() {
            author.setId("driver-1");
            assertThrows(ForbiddenException.class, () -> validator.validateRequest(author, ride, "Hotel"));
        }

        @Test
        @DisplayName("Deve falhar se o ponto de embarque for inválido")
        void shouldThrowBadRequestWhenPickupPointNotFound() {
            assertThrows(BadRequestException.class, () -> validator.validateRequest(author, ride, "Rodoviária"));
        }
    }

    @Nested
    @DisplayName("Validação de Aceite/Rejeição (validateAcceptance/Rejection)")
    class ProcessTests {

        @Test
        @DisplayName("Deve falhar se quem valida não for o motorista")
        void shouldThrowForbiddenWhenNotDriver() {
            assertThrows(ForbiddenException.class, () -> validator.validateAcceptance(participant, "outro-id"));
            assertThrows(ForbiddenException.class, () -> validator.validateRejection(participant, "outro-id"));
        }

        @Test
        @DisplayName("Deve falhar se a solicitação já foi processada")
        void shouldThrowConflictWhenAlreadyProcessed() {
            participant.setParticipantStatus(ParticipantStatus.ACCEPTED);
            assertThrows(ConflictException.class, () -> validator.validateAcceptance(participant, "driver-1"));
        }
    }

    @Nested
    @DisplayName("Validação de Cancelamento (validateCancelling)")
    class CancelTests {

        @Test
        @DisplayName("Deve passar se o passageiro cancelar antes da partida")
        void shouldPassWhenValidCancellation() {
            assertDoesNotThrow(() -> validator.validateCancelling(participant, "user-1"));
        }

        @Test
        @DisplayName("Deve falhar se o passageiro não for o dono da solicitação")
        void shouldThrowForbiddenWhenNotOwner() {
            assertThrows(ForbiddenException.class, () -> validator.validateCancelling(participant, "outro-user"));
        }

        @Test
        @DisplayName("Deve falhar se tentar cancelar após o horário de partida")
        void shouldThrowConflictAfterDeparture() {
            ride.setDepartureTime(LocalDateTime.now().minusMinutes(1));
            assertThrows(ConflictException.class, () -> validator.validateCancelling(participant, "user-1"));
        }
    }
}