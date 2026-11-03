package com.ifride.core.driver;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.repository.UserRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.BDDAssertions.then;
import static org.awaitility.Awaitility.await;

class DriverApprovalFlowIT extends BaseIT {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DriverRepository driverRepository;
    @Autowired
    private DriverApplicationRepository applicationRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User passenger;
    private String adminToken;
    private String passengerToken;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setName("Admin Teste");
        admin.setEmail("admin-" + UUID.randomUUID() + "@ifride.com");
        admin.setPassword(passwordEncoder.encode("senha123"));
        admin.setRole(Role.ADMIN);
        admin.setCpfEncrypted("encrypted_cpf_admin_" + UUID.randomUUID());
        admin.setBirthDate(LocalDate.of(1980, 1, 1));
        admin.setEmailVerified(true);
        userRepository.save(admin);

        passenger = new User();
        passenger.setId(UUID.randomUUID().toString());
        passenger.setName("Passageiro Teste");
        passenger.setEmail("passenger-" + UUID.randomUUID() + "@ifride.com");
        passenger.setPassword(passwordEncoder.encode("senha123"));
        passenger.setRole(Role.PASSENGER);
        passenger.setCpfEncrypted("encrypted_cpf_pass_" + UUID.randomUUID());
        passenger.setBirthDate(LocalDate.of(2000, 1, 1));
        passenger.setEmailVerified(true);
        userRepository.save(passenger);

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

        then(createAppResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

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