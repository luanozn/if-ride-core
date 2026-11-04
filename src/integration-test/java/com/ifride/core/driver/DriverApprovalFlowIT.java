package com.ifride.core.driver;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.driver.model.dto.DriverApplicationRequestDTO;
import com.ifride.core.driver.model.enums.CnhCategory;
import com.ifride.core.driver.model.enums.DriverApplicationStatus;
import com.ifride.core.driver.repository.DriverApplicationRepository;
import com.ifride.core.driver.repository.DriverRepository;
import com.ifride.core.shared.test.BaseIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.BDDAssertions.then;
import static org.awaitility.Awaitility.await;

class DriverApprovalFlowIT extends BaseIT {

    @Autowired
    private DriverRepository driverRepository;
    @Autowired
    private DriverApplicationRepository applicationRepository;

    private User passenger;
    private String adminToken;
    private String passengerToken;

    @BeforeEach
    void setUp() {

        User admin = createUser(Role.ADMIN);
        passenger = createUser(Role.PASSENGER);

        adminToken = getToken(admin.getEmail());
        passengerToken = getToken(passenger.getEmail());
    }

    @Test
    @DisplayName("Deve aprovar motorista, criar perfil, alterar role e expurgar CNH (LGPD) assincronamente")
    void shouldCompleteDriverApprovalFlow() {
        // ==========================================
        // 1. PASSAGEIRO: Solicita virar motorista
        // ==========================================
        var appRequest = new DriverApplicationRequestDTO(
                passenger.getId(),
                "12345678901",
                CnhCategory.B,
                "1234",
                LocalDate.now().plusYears(5)
        );

        var createAppResponse = rest.exchange(
                "/v1/driver-requests",
                HttpMethod.POST,
                new HttpEntity<>(appRequest, createHeaders(passengerToken)),
                Void.class
        );

        then(createAppResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        var pendingApp = applicationRepository.findAllByRequesterOrderByCreatedAtDesc(passenger).getFirst();
        then(pendingApp.getApplicationStatus()).isEqualTo(DriverApplicationStatus.PENDING);
        then(pendingApp.getCnhSecurityNumberEncrypted()).isNotNull();

        // ==========================================
        // 2. ADMIN: Aprova a solicitação
        // ==========================================
        var approveResponse = rest.exchange(
                "/v1/driver-requests/" + passenger.getId() + "/approve",
                HttpMethod.PATCH,
                new HttpEntity<>(createHeaders(adminToken)),
                Void.class
        );
        then(approveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // ==========================================
        // 3. VERIFICAÇÃO ASSÍNCRONA (Event-Driven)
        // ==========================================

        await().atMost(Duration.of(5, ChronoUnit.SECONDS))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    User updatedPassenger = userRepository.findById(passenger.getId()).orElseThrow();
                    then(updatedPassenger.getRole()).isEqualTo(Role.DRIVER);
                    boolean driverExists = driverRepository.existsById(passenger.getId());
                    then(driverExists).isTrue();
                    var approvedApp = applicationRepository.findById(pendingApp.getId()).orElseThrow();
                    then(approvedApp.getApplicationStatus()).isEqualTo(DriverApplicationStatus.APPROVED);
                    then(approvedApp.getCnhSecurityNumberEncrypted()).isNull();
                });
    }
}