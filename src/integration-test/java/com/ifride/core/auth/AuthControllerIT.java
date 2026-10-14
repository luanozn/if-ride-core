package com.ifride.core.auth;

import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.auth.model.dto.RegisterResponseDTO;
import com.ifride.core.auth.model.entity.ActionToken;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.TokenType;
import com.ifride.core.auth.repository.ActionTokenRepository;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.shared.config.LocalEmailService;
import com.ifride.core.shared.test.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;

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
        var requestBody = new RegisterRequestDTO("john.doe@email.com", "mySuperSecretPassword", "John Doe","123.447.230-99");

        var response = rest.postForEntity("/v1/auth/register", requestBody, RegisterResponseDTO.class);

        then(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));

        await().atMost(Duration.of(5, ChronoUnit.SECONDS))
                .pollInterval(Duration.ofMillis(50))
                .untilAsserted(() -> then(localEmailService.getCalls()).hasSize(1));

        User user = userService.findByEmail("john.doe@email.com");
        then(user).isNotNull();

        await().atMost(Duration.of(5, ChronoUnit.SECONDS)).pollInterval(Duration.ofMillis(50)).untilAsserted(() -> {
            ActionToken token = actionTokenRepository.findByUserAndType(user, TokenType.EMAIL_VERIFICATION).orElse(null);
            then(token).isNotNull();
            then(token.getExpires()).isAfter(Instant.now(clock));

            var email = localEmailService.lastCallTo("john.doe@email.com").orElseThrow();
            then(email.subject()).containsIgnoringCase("verificação");
            then(email.body()).contains(token.getToken());
        });

    }
}
