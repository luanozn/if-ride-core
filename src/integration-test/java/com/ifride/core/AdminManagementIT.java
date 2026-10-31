package com.ifride.core;

import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.auth.model.dto.RegisterResponseDTO;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.repository.UserRepository;
import com.ifride.core.shared.config.ErrorResponse;
import com.ifride.core.shared.test.BaseIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.BDDAssertions.then;

class AdminManagementIT extends BaseIT {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Deve impedir a exclusão do último administrador e permitir exclusão se houver outro")
    void shouldProtectLastAdminInvariant() {
        User admin = new User();
        admin.setName("Admin Único");
        admin.setEmail("admin-" + UUID.randomUUID() + "@ifride.com");
        admin.setPassword(passwordEncoder.encode("senha123"));
        admin.setRole(Role.ADMIN);
        admin.setCpfEncrypted("encrypted_cpf_" + UUID.randomUUID());
        admin.setBirthDate(LocalDate.of(1980, 1, 1));
        admin.setEmailVerified(true);
        userRepository.save(admin);

        String token = getToken(admin.getEmail());

        var failResponse = rest.exchange(
                "/v1/users/me",
                HttpMethod.DELETE,
                new HttpEntity<>(createHeaders(token)),
                ErrorResponse.class
        );

        then(failResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        then(failResponse.getBody()).isNotNull();
        then(failResponse.getBody().getMessage()).contains("último administrador");

        then(userRepository.existsById(admin.getId())).isTrue();

        var createAdminRequest = new RegisterRequestDTO(
                "admin2-" + UUID.randomUUID() + "@ifride.com", "senha123", "Admin 2", "053.440.630-08", LocalDate.of(1990, 1, 1)
        );
        var createResponse = rest.exchange(
                "/v1/admins/users/admin",
                HttpMethod.POST,
                new HttpEntity<>(createAdminRequest, createHeaders(token)),
                RegisterResponseDTO.class
        );
        then(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String secondAdminId = createResponse.getBody().id();

        var successResponse = rest.exchange(
                "/v1/admins/" + secondAdminId,
                HttpMethod.DELETE,
                new HttpEntity<>(createHeaders(token)),
                Void.class
        );

        then(successResponse.getStatusCode().is2xxSuccessful()).isTrue();
        then(userRepository.existsById(secondAdminId)).isFalse();
    }
}