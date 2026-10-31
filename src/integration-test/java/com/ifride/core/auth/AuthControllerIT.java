package com.ifride.core.auth;

import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.auth.model.dto.RegisterResponseDTO;
import com.ifride.core.auth.model.entity.ActionToken;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.TokenType;
import com.ifride.core.auth.repository.ActionTokenRepository;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.shared.config.LocalEmailService;
import com.ifride.core.shared.exceptions.api.ApiException;
import com.ifride.core.shared.test.IntegrationTest;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.BDDAssertions.then;
import static org.awaitility.Awaitility.await;

public class AuthControllerIT extends IntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private ActionTokenRepository actionTokenRepository;

    @Autowired
    private Clock clock;

    @Autowired
    private LocalEmailService localEmailService;

    @AfterEach
    public void cleanUp() {
        if(localEmailService != null) {
            localEmailService.clear();
        }
    }

    @Test
    @DisplayName("Deve registrar o usuário e ativar o fluxo de geração de token e envio de e-mail")
    public void shouldRegisterUserAndTriggerTokenGeneration() {
        var requestBody = new RegisterRequestDTO("john.doe@email.com", "mySuperSecretPassword", "John Doe","123.447.230-99", LocalDate.of(1990, 1, 1));

        var response = rest.postForEntity("/v1/auth/register", requestBody, RegisterResponseDTO.class);

        then(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        await().atMost(Duration.of(5, ChronoUnit.SECONDS))
                .pollInterval(Duration.ofMillis(50))
                .untilAsserted(() -> then(localEmailService.getCalls()).hasSize(1));

        User user = userService.findByEmail("john.doe@email.com");
        then(user).isNotNull();
        then(user.getCpfEncrypted()).isEqualTo("12344723099");

        ActionToken token = actionTokenRepository.findByUserAndType(user, TokenType.EMAIL_VERIFICATION).orElse(null);
        then(token).isNotNull();
        then(token.getExpires()).isAfter(Instant.now(clock));

        var email = localEmailService.lastCallTo("john.doe@email.com").orElseThrow();
        then(email.subject()).containsIgnoringCase("verificação");
        then(email.body()).contains(token.getToken());
    }

    @Nested
    @DisplayName("Fluxos de exceção")
    class ExceptionFlows {
        @Test
        @DisplayName("Deve lançar BadRequestException se o CPF da requisição for inválido")
        public void shouldThrowExceptionWhenDocumentIsInvalid() {
            var requestBody = new RegisterRequestDTO("john.dolmayan@email.com", "mySuperSecretPassword", "John Dolmayan","123.456.789-11", LocalDate.of(1990, 1, 1));

            var response = rest.postForEntity("/v1/auth/register", requestBody, ApiException.class);

            then(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            then(response.getBody()).isNotNull();
            then(response.getBody().getMessage()).containsIgnoringCase("O CPF inserido é inválido.");
        }

        @Test
        @DisplayName("Deve lançar ConflictException se o CPF da requisição já existir no banco")
        public void shouldThrowConflictWhenDocumentAlreadyExists() {
            var successRequest = new RegisterRequestDTO("serj.tankian@email.com", "mySuperSecretPassword", "Serj Tankian","464.093.640-08", LocalDate.of(1990, 1, 1));
            var successResponse = rest.postForEntity("/v1/auth/register", successRequest, ApiException.class);
            then(successResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

            var failRequest = new RegisterRequestDTO("serj@email.com" ,"mySuperSecretPassword", "Serj Tankian","464.093.640-08", LocalDate.of(1990, 1, 1));
            var failResponse = rest.postForEntity("/v1/auth/register", failRequest, ApiException.class);
            then(failResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            then(failResponse.getBody()).isNotNull();
            then(failResponse.getBody().getMessage()).containsIgnoringCase("Não é possível cadastrar o usuário. O CPF 464.093.640-08 já está cadastrado");
        }

        @Test
        @DisplayName("Deve lançar ConflictException se o email da requisição já existir no banco")
        public void shouldThrowConflictWhenEmailAlreadyExists() {
            var successRequest = new RegisterRequestDTO("daron.malakian@email.com", "mySuperSecretPassword", "Daron Malakian","464.093.640-08", LocalDate.of(1990, 1, 1));
            var successResponse = rest.postForEntity("/v1/auth/register", successRequest, ApiException.class);
            then(successResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

            var failRequest = new RegisterRequestDTO("daron.malakian@email.com" ,"mySuperSecretPassword", "Daron Malakian","690.500.690-45", LocalDate.of(1990, 1, 1));
            var failResponse = rest.postForEntity("/v1/auth/register", failRequest, ApiException.class);
            then(failResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            then(failResponse.getBody()).isNotNull();
            then(failResponse.getBody().getMessage()).containsIgnoringCase("Não é possível cadastrar o usuário. O email já está em uso!");
        }
    }
}
