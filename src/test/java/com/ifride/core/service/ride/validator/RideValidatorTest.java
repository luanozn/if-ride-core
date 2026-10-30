package com.ifride.core.service.ride.validator;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.driver.model.entity.Driver;
import com.ifride.core.driver.model.entity.Vehicle;
import com.ifride.core.ride.model.Ride;
import com.ifride.core.ride.model.dto.RideRequestDTO;
import com.ifride.core.ride.model.enums.RideStatus;
import com.ifride.core.ride.repository.RideRepository;
import com.ifride.core.ride.service.validators.RideValidator;
import com.ifride.core.shared.exceptions.api.BadRequestException;
import com.ifride.core.shared.exceptions.api.ConflictException;
import com.ifride.core.shared.exceptions.api.ForbiddenException;
import com.ifride.core.shared.utils.DateTimeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RideValidatorTest {

    @Mock
    private RideRepository rideRepository;

    @Mock
    private DateTimeUtils dateTimeUtils;

    @InjectMocks
    private RideValidator validator;

    private Driver driver;
    private Vehicle vehicle;
    private User driverUser;

    @BeforeEach
    void setUp() {
        driverUser = new User();
        driverUser.setId("driver-1");
        driverUser.setEmail("driver@ifride.com");

        driver = new Driver();
        driver.setId("driver-1");
        driver.setUser(driverUser);

        vehicle = new Vehicle();
        vehicle.setId("vehicle-1");
        vehicle.setCapacity(4);
        vehicle.setOwner(driver);
    }

    @Nested
    @DisplayName("Regras de Criação de Carona")
    class RideCreationRules {

        @Test
        @DisplayName("Deve permitir a criação quando os dados estão corretos e não há conflitos")
        void shouldAllowValidCreation() {
            var dto = new RideRequestDTO("vehicle-1", "A", "B", null, Instant.now().plus(2, ChronoUnit.HOURS), 3, BigDecimal.TEN, false, null, null);

            given(dateTimeUtils.getDepartureTime(any())).willReturn(Instant.now().plus(2, ChronoUnit.HOURS));
            given(rideRepository.existsOverlap(any(), any(), any())).willReturn(false);

            assertThatCode(() -> validator.validateRideCreation(driver, vehicle, dto))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve barrar quando o veículo não pertence ao motorista logado")
        void shouldBlockThirdPartyVehicle() {
            var otherDriver = new Driver();
            otherDriver.setId("driver-2");
            vehicle.setOwner(otherDriver);

            var dto = new RideRequestDTO("vehicle-1", "A", "B", null, Instant.now().plus(2, ChronoUnit.HOURS), 3, BigDecimal.TEN, false, null, null);

            // Não precisa mockar o dateTimeUtils aqui, pois a exceção é lançada antes (checkVehicleBelongsToDriver)
            assertThatThrownBy(() -> validator.validateRideCreation(driver, vehicle, dto))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("não pertence ao usuário");
        }

        @Test
        @DisplayName("Deve barrar quando a data de partida for no passado")
        void shouldBlockPastDepartureTime() {
            var dto = new RideRequestDTO("vehicle-1", "A", "B", null, Instant.now().minus(1, ChronoUnit.HOURS), 3, BigDecimal.TEN, false, null, null);

            given(dateTimeUtils.getDepartureTime(any())).willReturn(Instant.now().minus(1, ChronoUnit.HOURS));

            assertThatThrownBy(() -> validator.validateRideCreation(driver, vehicle, dto))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("não pode ser no passado");
        }

        @Test
        @DisplayName("Deve barrar quando as vagas excederem a capacidade do veículo")
        void shouldBlockSeatsExceedingCapacity() {
            var dto = new RideRequestDTO("vehicle-1", "A", "B", null, Instant.now().plus(2, ChronoUnit.HOURS), 5, BigDecimal.TEN, false, null, null); // Capacity: 4

            given(dateTimeUtils.getDepartureTime(any())).willReturn(Instant.now().plus(2, ChronoUnit.HOURS));

            assertThatThrownBy(() -> validator.validateRideCreation(driver, vehicle, dto))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("excede a capacidade");
        }

        @Test
        @DisplayName("Deve barrar quando houver conflito de horário (sobreposição de 1 hora)")
        void shouldBlockTimeConflict() {
            var dto = new RideRequestDTO("vehicle-1", "A", "B", null, Instant.now().plus(2, ChronoUnit.HOURS), 3, BigDecimal.TEN, false, null, null);

            given(dateTimeUtils.getDepartureTime(any())).willReturn(Instant.now().plus(2, ChronoUnit.HOURS));
            given(rideRepository.existsOverlap(any(), any(), any())).willReturn(true);

            assertThatThrownBy(() -> validator.validateRideCreation(driver, vehicle, dto))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Conflito de Horário");
        }

        @Test
        @DisplayName("Deve exigir dia e hora se a carona for recorrente")
        void shouldRequireRecurrenceData() {
            var withoutDay = new RideRequestDTO("vehicle-1", "A", "B", null, null, 3, BigDecimal.TEN, true, null, LocalTime.NOON);

            // A exceção é lançada no início do fluxo (checkRecurrency), antes de usar o DateTimeUtils
            assertThatThrownBy(() -> validator.validateRideCreation(driver, vehicle, withoutDay))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("dia da semana é obrigatório");

            var withoutTime = new RideRequestDTO("vehicle-1", "A", "B", null, null, 3, BigDecimal.TEN, true, DayOfWeek.MONDAY, null);

            assertThatThrownBy(() -> validator.validateRideCreation(driver, vehicle, withoutTime))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("horário de partida é obrigatório");
        }
    }

    @Nested
    @DisplayName("Regras de Início de Carona")
    class RideStartRules {

        @Test
        @DisplayName("Deve permitir o início até 30 minutos antes da partida")
        void shouldAllowStartCloseToDeparture() {
            var ride = new Ride();
            ride.setDriver(driver);
            ride.setRideStatus(RideStatus.SCHEDULED);
            ride.setDepartureTime(Instant.now().plus(20, ChronoUnit.MINUTES));

            given(rideRepository.existsByDriverIdAndRideStatus("driver-1", RideStatus.IN_PROGRESS)).willReturn(false);

            assertThatCode(() -> validator.validateRideUpdate(ride, driverUser, RideStatus.IN_PROGRESS))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve barrar o início se faltar mais de 30 minutos para a partida")
        void shouldBlockEarlyStart() {
            var ride = new Ride();
            ride.setDriver(driver);
            ride.setRideStatus(RideStatus.SCHEDULED);
            ride.setDepartureTime(Instant.now().plus(1, ChronoUnit.HOURS));

            assertThatThrownBy(() -> validator.validateRideUpdate(ride, driverUser, RideStatus.IN_PROGRESS))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("só pode ser iniciada 30 minutos antes");
        }

        @Test
        @DisplayName("Deve barrar se o motorista já tiver outra carona em andamento")
        void shouldBlockBusyDriver() {
            var ride = new Ride();
            ride.setDriver(driver);
            ride.setRideStatus(RideStatus.SCHEDULED);
            ride.setDepartureTime(Instant.now().plus(20, ChronoUnit.MINUTES));

            given(rideRepository.existsByDriverIdAndRideStatus("driver-1", RideStatus.IN_PROGRESS)).willReturn(true);

            assertThatThrownBy(() -> validator.validateRideUpdate(ride, driverUser, RideStatus.IN_PROGRESS))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("já possui uma carona em andamento");
        }
    }

    @Nested
    @DisplayName("Regras de Finalização de Carona")
    class RideFinishRules {

        @Test
        @DisplayName("Deve permitir finalizar uma carona que está IN_PROGRESS")
        void shouldAllowFinishWhenInProgress() {
            var ride = new Ride();
            ride.setDriver(driver);
            ride.setRideStatus(RideStatus.IN_PROGRESS);

            assertThatCode(() -> validator.validateRideUpdate(ride, driverUser, RideStatus.FINISHED))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve barrar a finalização de uma carona que não foi iniciada")
        void shouldBlockFinishWhenNotInProgress() {
            var ride = new Ride();
            ride.setDriver(driver);
            ride.setRideStatus(RideStatus.SCHEDULED); // Ainda não iniciou

            assertThatThrownBy(() -> validator.validateRideUpdate(ride, driverUser, RideStatus.FINISHED))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Não é possível finalizar uma carona não iniciada");
        }

        @Test
        @DisplayName("Deve barrar a alteração de status de uma carona já FINALIZADA")
        void shouldBlockStatusChangeWhenAlreadyFinished() {
            var ride = new Ride();
            ride.setDriver(driver);
            ride.setRideStatus(RideStatus.FINISHED);

            assertThatThrownBy(() -> validator.validateRideUpdate(ride, driverUser, RideStatus.CANCELLED))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Não é possível alterar o status de uma carona FINALIZADA");
        }
    }

    @Nested
    @DisplayName("Regras de Desativação de Recorrência")
    class DisableRecurrenceRules {

        @Test
        @DisplayName("Deve permitir desativar recorrência de uma carona válida")
        void shouldAllowDisableWhenValid() {
            var ride = new Ride();
            ride.setDriver(driver);
            ride.setRecurrent(true);
            ride.setRideStatus(RideStatus.SCHEDULED);

            assertThatCode(() -> validator.validateDisableRecurrence(ride, driverUser))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve barrar se a carona não for recorrente")
        void shouldBlockWhenRideIsNotRecurrent() {
            var ride = new Ride();
            ride.setDriver(driver);
            ride.setRecurrent(false);

            assertThatThrownBy(() -> validator.validateDisableRecurrence(ride, driverUser))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Esta carona não é recorrente");
        }

        @Test
        @DisplayName("Deve barrar se a carona já estiver finalizada ou cancelada")
        void shouldBlockWhenRideIsFinishedOrCancelled() {
            var ride = new Ride();
            ride.setDriver(driver);
            ride.setRecurrent(true);
            ride.setRideStatus(RideStatus.CANCELLED);

            assertThatThrownBy(() -> validator.validateDisableRecurrence(ride, driverUser))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("já finalizada ou cancelada");
        }
    }
}