package com.ifride.core.shared.test;

import com.ifride.core.auth.model.dto.LoginRequestDTO;
import com.ifride.core.auth.model.dto.LoginResponseDTO;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.BDDAssertions.then;

public abstract class BaseIT extends IntegrationTest {

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE users, drivers, vehicles, rides, ride_participants, driver_applications, tokens, audit_logs CASCADE");
    }

    @Value("${app.gateway.origin}")
    protected String gatewayOriginHash;

    protected User createUser(Role role) {
        return createUser("user-" + UUID.randomUUID() + "@ifride.com", role);
    }

    protected User createUser(String email, Role role) {
        User user = new User();
        user.setName("User " + role);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("senha123"));
        user.setRole(role);
        user.setCpfEncrypted("encrypted_cpf_" + UUID.randomUUID());
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    protected String getToken(String email) {
        var loginRequest = new LoginRequestDTO(email, "senha123");
        var response = rest.exchange(
                "/v1/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest, createHeaders(null)),
                LoginResponseDTO.class
        );
        then(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().token();
    }

    protected HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        headers.set("X-Origin", gatewayOriginHash);
        return headers;
    }
}