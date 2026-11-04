package com.ifride.core.shared.test;

import com.ifride.core.auth.model.dto.LoginRequestDTO;
import com.ifride.core.auth.model.dto.LoginResponseDTO;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.repository.UserRepository;
import com.ifride.core.driver.model.entity.Driver;
import com.ifride.core.driver.model.enums.CnhCategory;
import com.ifride.core.shared.services.DeterministicEncryptorService;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.BDDAssertions.then;

public abstract class BaseIT extends IntegrationTest {

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    protected EntityManager entityManager;
    @Autowired
    protected DeterministicEncryptorService crypto;

    @Autowired
    protected TransactionTemplate transactionTemplate;

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
        user.setCpfEncrypted(generateEncryptedDocument());
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
        return Objects.requireNonNull(response.getBody()).token();
    }

    protected HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        headers.set("X-Origin", gatewayOriginHash);
        return headers;
    }

    protected Driver createDriverTo(User detachedUser) {
        Driver driver = new Driver();
        driver.setCnhCategory(CnhCategory.AB);
        driver.setCnhNumberEncrypted(generateEncryptedDocument());
        driver.setCnhExpiration(LocalDate.now().plusYears(5));

        transactionTemplate.executeWithoutResult(status -> {
            User managedUser = entityManager.getReference(User.class, detachedUser.getId());
            driver.setUser(managedUser);
            entityManager.persist(driver);
        });

        return driver;
    }

    private String generateEncryptedDocument() {
        Random random = new Random();
        return crypto.encrypt(String.format("123456%s", random.nextInt(10000, 99999)));
    }
}
