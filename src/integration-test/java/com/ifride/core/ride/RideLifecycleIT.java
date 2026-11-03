package com.ifride.core.ride;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.repository.UserRepository;
import com.ifride.core.driver.model.entity.Driver;
import com.ifride.core.driver.model.entity.Vehicle;
import com.ifride.core.driver.model.enums.CnhCategory;
import com.ifride.core.driver.repository.DriverRepository;
import com.ifride.core.driver.repository.VehicleRepository;
import com.ifride.core.ride.model.Ride;
import com.ifride.core.ride.model.dto.RideParticipantRequestDTO;
import com.ifride.core.ride.model.dto.RideParticipantResponseDTO;
import com.ifride.core.ride.model.dto.RideRequestDTO;
import com.ifride.core.ride.model.dto.RideResponseDTO;
import com.ifride.core.ride.model.enums.ParticipantStatus;
import com.ifride.core.ride.model.enums.RideStatus;
import com.ifride.core.ride.repository.RideParticipantRepository;
import com.ifride.core.ride.repository.RideRepository;
import com.ifride.core.shared.test.BaseIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.BDDAssertions.then;

class RideLifecycleIT extends BaseIT {

    @Autowired private UserRepository userRepository;
    @Autowired private DriverRepository driverRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private RideRepository rideRepository;
    @Autowired private RideParticipantRepository participantRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User driverUser;
    private User passengerUser;
    private User passengerUser2;
    private Vehicle vehicle;

    private String driverToken;
    private String passengerToken;
    private String passenger2Token;

    @BeforeEach
    void setUp() {
        driverUser = createUser("driver-" + UUID.randomUUID() + "@ifride.com", Role.DRIVER);
        Driver driver = new Driver();
        driver.setId(driverUser.getId());
        driver.setUser(driverUser);
        driver.setCnhCategory(CnhCategory.B);
        driver.setCnhNumberEncrypted("encrypted_cnh");
        driver.setCnhExpiration(LocalDate.now().plusYears(5));
        driverRepository.save(driver);

        vehicle = new Vehicle();
        vehicle.setOwner(driver);
        vehicle.setModel("Fiat Argo");
        vehicle.setPlate("ABC1D23");
        vehicle.setCapacity(4);
        vehicleRepository.save(vehicle);

        passengerUser = createUser("passenger1-" + UUID.randomUUID() + "@ifride.com", Role.PASSENGER);
        passengerUser2 = createUser("passenger2-" + UUID.randomUUID() + "@ifride.com", Role.PASSENGER);

        driverToken = getToken(driverUser.getEmail());
        passengerToken = getToken(passengerUser.getEmail());
        passenger2Token = getToken(passengerUser2.getEmail());
    }

    @Nested
    @DisplayName("Ciclo de Vida da Carona e Concorrência de Vagas")
    class RideLifecycleAndConcurrency {

        @Test
        @DisplayName("Deve criar carona, aceitar passageiro, lotar e impedir overbooking")
        void shouldCreateRideAcceptPassengerAndPreventOverbooking() {
            // ==========================================
            // 1. DRIVER: Ofertar uma carona com apenas 1 VAGA
            // ==========================================
            var rideRequest = new RideRequestDTO(
                    vehicle.getId(),
                    "Campus Central",
                    "Terminal Rodoviário",
                    List.of("Ponto A"),
                    Instant.now().plus(2, ChronoUnit.HOURS),
                    1,
                    BigDecimal.TEN,
                    false,
                    null,
                    null
            );

            var createRideResponse = rest.exchange(
                    "/v1/rides",
                    HttpMethod.POST,
                    new HttpEntity<>(rideRequest, createHeaders(driverToken)),
                    RideResponseDTO.class
            );

            then(createRideResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            then(createRideResponse.getBody().availableSeats()).isEqualTo(1);
            String rideId = createRideResponse.getBody().id();

            // ==========================================
            // 2. PASSAGEIRO 1: Solicita a vaga
            // ==========================================
            var seatRequest = new RideParticipantRequestDTO("Ponto A");
            var requestSeatResponse = rest.exchange(
                    "/v1/rides/" + rideId + "/request-seat",
                    HttpMethod.POST,
                    new HttpEntity<>(seatRequest, createHeaders(passengerToken)),
                    RideParticipantResponseDTO.class
            );

            then(requestSeatResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            then(requestSeatResponse.getBody().status()).isEqualTo(ParticipantStatus.PENDING);
            String participantId = requestSeatResponse.getBody().id();

            // ==========================================
            // 3. DRIVER: Aceita o passageiro
            // ==========================================
            var acceptResponse = rest.exchange(
                    "/v1/ride-participant/" + participantId + "/accept",
                    HttpMethod.PATCH,
                    new HttpEntity<>(createHeaders(driverToken)),
                    Void.class
            );

            then(acceptResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            // ==========================================
            // 4. VERIFICAÇÃO: Carona deve estar FULL (lotada) e sem vagas
            // ==========================================
            Ride updatedRide = rideRepository.findById(rideId).orElseThrow();
            then(updatedRide.getAvailableSeats()).isZero();
            then(updatedRide.getRideStatus()).isEqualTo(RideStatus.FULL);

            // ==========================================
            // 5. PROTEÇÃO CONTRA OVERBOOKING (PASSAGEIRO 2)
            // ==========================================

            var overbookingResponse = rest.exchange(
                    "/v1/rides/" + rideId + "/request-seat",
                    HttpMethod.POST,
                    new HttpEntity<>(seatRequest, createHeaders(passenger2Token)),
                    Void.class
            );

            then(overbookingResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("Deve iniciar e finalizar a carona alterando os estados corretamente")
        void shouldStartAndFinishRide() {
            var rideRequest = new RideRequestDTO(vehicle.getId(), "A", "B", List.of(),
                    Instant.now().plus(20, ChronoUnit.MINUTES), 3, BigDecimal.ZERO, false, null, null);

            var createResponse = rest.exchange("/v1/rides", HttpMethod.POST,
                    new HttpEntity<>(rideRequest, createHeaders(driverToken)), RideResponseDTO.class);
            String rideId = createResponse.getBody().id();

            var startResponse = rest.exchange("/v1/rides/" + rideId + "/start", HttpMethod.PATCH,
                    new HttpEntity<>(createHeaders(driverToken)), Void.class);
            then(startResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            then(rideRepository.findById(rideId).orElseThrow().getRideStatus()).isEqualTo(RideStatus.IN_PROGRESS);

            var finishResponse = rest.exchange("/v1/rides/" + rideId + "/finish", HttpMethod.PATCH,
                    new HttpEntity<>(createHeaders(driverToken)), Void.class);
            then(finishResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            then(rideRepository.findById(rideId).orElseThrow().getRideStatus()).isEqualTo(RideStatus.FINISHED);
        }
    }
}