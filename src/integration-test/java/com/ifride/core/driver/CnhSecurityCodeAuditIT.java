package com.ifride.core.driver;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.driver.model.entity.DriverApplication;
import com.ifride.core.driver.model.enums.CnhCategory;
import com.ifride.core.driver.model.enums.DriverApplicationStatus;
import com.ifride.core.driver.repository.DriverApplicationRepository;
import com.ifride.core.shared.audit.AuditLog;
import com.ifride.core.shared.audit.repository.AuditLogRepository;
import com.ifride.core.shared.services.DeterministicEncryptorService;
import com.ifride.core.shared.services.RandomizedEncryptorService;
import com.ifride.core.shared.test.BaseIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.BDDAssertions.then;

class CnhSecurityCodeAuditIT extends BaseIT {

    @Autowired private DriverApplicationRepository applicationRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private DeterministicEncryptorService deterministicCrypto;
    @Autowired private RandomizedEncryptorService randomizedCrypto;

    private User admin;
    private User passenger;
    private DriverApplication application;
    private String adminToken;

    @BeforeEach
    void setUp() {
        admin = createUser("admin-" + UUID.randomUUID() + "@ifride.com", Role.ADMIN);
        passenger = createUser("pass-" + UUID.randomUUID() + "@ifride.com", Role.PASSENGER);

        // Cria a aplicação PENDING com dados reais criptografados
        application = new DriverApplication();
        application.setRequester(passenger);
        application.setApplicationStatus(DriverApplicationStatus.PENDING);
        application.setCnhNumberEncrypted(deterministicCrypto.encrypt("12345678901"));
        application.setCnhSecurityNumberEncrypted(randomizedCrypto.encrypt("9876"));
        application.setCnhCategory(CnhCategory.B);
        application.setCnhExpiration(LocalDate.now().plusYears(5));
        applicationRepository.save(application);

        adminToken = getToken(admin.getEmail());
    }

    @Test
    @DisplayName("Deve revelar código de segurança, registrar auditoria com IP e expurgar dado (LGPD)")
    void shouldRevealSecurityCodeAndGenerateAuditLog() {
        HttpHeaders headers = createHeaders(adminToken);
        headers.set("X-Forwarded-For", "189.45.12.10");

        var response = rest.exchange(
                "/v1/driver-requests/" + application.getId() + "/security-code",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        then(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<AuditLog> logs = auditLogRepository.findAll();
        then(logs).hasSize(1);

        AuditLog log = logs.get(0);
        then(log.getAction()).isEqualTo("VIEW_CNH_SECURITY_CODE");
        then(log.getTargetId()).isEqualTo(application.getId());
        then(log.getAuthorId()).isEqualTo(admin.getId());
        then(log.getIpAddress()).isEqualTo("189.45.12.10"); // Prova o rastreio de IP
    }
}