package com.ifride.core.auth;

import com.ifride.core.auth.model.dto.LoginRequestDTO;
import com.ifride.core.auth.model.dto.LoginResponseDTO;
import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.auth.model.dto.RegisterResponseDTO;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.repository.UserRepository;
import com.ifride.core.shared.config.LocalEmailService;
import com.ifride.core.shared.exceptions.api.ApiException;
import com.ifride.core.shared.services.DeterministicEncryptorService;
import com.ifride.core.shared.test.BaseIT;
import com.ifride.core.shared.test.IntegrationTest;
import com.ifride.core.shared.utils.CpfViewConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.BDDAssertions.then;
import static org.awaitility.Awaitility.await;

class AuthFlowIT extends BaseIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeterministicEncryptorService crypto;

    @Autowired
    private LocalEmailService localEmailService;

    private final String VALID_CPF = "053.440.630-08";

    @AfterEach
    void tearDown() {
        if (localEmailService != null) {
            localEmailService.clear();
        }
    }

    @Nested
    @DisplayName("Fluxo Completo de Autenticação")
    class FullAuthFlow {

        @Test
        @DisplayName("Deve registrar, validar e-mail assincronamente, logar e acessar rota protegida")
        void shouldRegisterVerifyLoginAndAccessProtectedRoute() {
            String uniqueEmail = "user-" + UUID.randomUUID() + "@ifride.com";
            var registerRequest = new RegisterRequestDTO(uniqueEmail, "senhaSegura123", "Usuário Teste", VALID_CPF, LocalDate.of(1995, 5, 20));

            var registerResponse = rest.exchange(
                    "/v1/auth/register",
                    HttpMethod.POST,
                    new HttpEntity<>(registerRequest, createHeaders(null)),
                    RegisterResponseDTO.class);

            then(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            then(registerResponse.getBody()).isNotNull();
            then(registerResponse.getBody().emailVerified()).isFalse();

            User user = userRepository.findByEmail(uniqueEmail).orElseThrow();
            String decryptedCpf = crypto.decrypt(user.getCpfEncrypted());
            then(CpfViewConverter.convertFormatted(decryptedCpf))
                    .isEqualTo(CpfViewConverter.convertFormatted(VALID_CPF));

            await().atMost(Duration.of(5, ChronoUnit.SECONDS))
                    .pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> then(localEmailService.getCalls()).hasSize(1));

            var email = localEmailService.lastCallTo(uniqueEmail).orElseThrow();
            String token = email.body().split("token=")[1].split("\"")[0];

            var verifyResponse = rest.exchange(
                    "/v1/auth/verify-email?token=" + token,
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders(null)),
                    String.class);
            then(verifyResponse.getStatusCode()).isEqualTo(HttpStatus.FOUND);
            then(verifyResponse.getHeaders().getLocation().toString()).contains("verification-success");

            User verifiedUser = userRepository.findByEmail(uniqueEmail).orElseThrow();
            then(verifiedUser.isEmailVerified()).isTrue();

            var loginRequest = new LoginRequestDTO(uniqueEmail, "senhaSegura123");
            var loginResponse = rest.exchange(
                    "/v1/auth/login",
                    HttpMethod.POST,
                    new HttpEntity<>(loginRequest, createHeaders(null)),
                    LoginResponseDTO.class);

            then(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            String jwtToken = loginResponse.getBody().token();
            then(jwtToken).isNotBlank();

            var protectedResponse = rest.exchange(
                    "/v1/driver-requests/me",
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders(token)),
                    ApiException.class
            );

            then(protectedResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Fluxos de exceção (Integração com Banco de Dados)")
    class ExceptionFlows {

        @Test
        @DisplayName("Deve lançar ConflictException (409) se o e-mail já existir no banco")
        void shouldThrowConflictWhenEmailAlreadyExists() {
            String uniqueEmail = "conflict-" + UUID.randomUUID() + "@ifride.com";
            var request1 = new RegisterRequestDTO(uniqueEmail, "senha123", "User 1", VALID_CPF, LocalDate.of(1990, 1, 1));

            var response1 = rest.exchange("/v1/auth/register", HttpMethod.POST, new HttpEntity<>(request1, createHeaders(null)), ApiException.class);

            then(response1.getStatusCode()).isEqualTo(HttpStatus.OK);

            var request2 = new RegisterRequestDTO(uniqueEmail, "outraSenha123", "User 2", "111.444.777-35", LocalDate.of(1990, 1, 1));
            var response2 = rest.exchange("/v1/auth/register", HttpMethod.POST, new HttpEntity<>(request2, createHeaders(null)), ApiException.class);

            then(response2.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            then(response2.getBody().getMessage()).containsIgnoringCase("O email já está em uso");
        }
    }

}