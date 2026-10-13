package com.ifride.core.service.auth.service;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.repository.ActionTokenRepository;
import com.ifride.core.auth.service.EmailVerificationTokenService;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.service.auth.utils.TokenResolveScenario;
import com.ifride.core.service.auth.utils.TokenResolveScenario.ResolveOutcome;
import com.ifride.core.shared.services.EmailService;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EmailVerificationTokenServiceTest {

    @Mock
    ActionTokenRepository tokenRepository;
    @Mock
    UserService userService;
    @Mock
    EmailService emailService;

    @InjectMocks
    EmailVerificationTokenService underTest;

    private static User from(String name, String email, String password, String cpf) {
        var user = new User();
        user.setEmail(email);
        user.setPassword(password);
        user.setName(name);
        user.setCpf(cpf);
        return user;
    }

    @Nested()
    @DisplayName("Ciclo de vida dos tokens")
    class TokenLifeCycleTests {
        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("tokenScenarios")
        void shouldResolveTokenCorrectly(TokenResolveScenario scenario) {
            var user = userWithId("user-1");
            var now = Instant.parse("2026-01-01T10:00:00Z");
            clock = new MutableClock(now, ZoneOffset.UTC);

            given(tokenRepository.findByUserAndType(user, TokenType.EMAIL_VERIFICATION))
                    .willReturn(scenario.existingToken());

            service.generateTokenAndSendEmail(user);

            // Verifica deleção
            if (scenario.shouldDelete()) {
                verify(tokenRepository).delete(scenario.existingToken().orElseThrow());
            } else {
                verify(tokenRepository, never()).delete(any());
            }

            // Verifica TTL do token salvo
            var savedCaptor = ArgumentCaptor.forClass(ActionToken.class);
            verify(tokenRepository).save(savedCaptor.capture());

            assertThat(savedCaptor.getValue().getExpires())
                    .as("TTL esperado para o cenário: %s", scenario.description())
                    .isEqualTo(now.plus(scenario.expectedTtl()));
        }

        static Stream<TokenResolveScenario> tokenScenarios() {
            var now = Instant.parse("2026-01-01T10:00:00Z");
            var futureToken = tokenExpiringAt(now.plus(Duration.ofHours(1)));
            var expiredToken = tokenExpiringAt(now.minus(Duration.ofHours(1)));
            var exactlyNowToken = tokenExpiringAt(now);

            return Stream.of(
                    new TokenResolveScenario(
                            "Token válido no futuro → reusa sem deletar",
                            Optional.of(futureToken),
                            ResolveOutcome.REUSE,
                            false,
                            Duration.ofHours(1)  // mantém o original
                    ),
                    new TokenResolveScenario(
                            "Token expirado → deleta e cria novo com TTL de 24h",
                            Optional.of(expiredToken),
                            ResolveOutcome.DELETE_AND_CREATE,
                            true,
                            Duration.ofHours(24)
                    ),
                    new TokenResolveScenario(
                            "Token expirando exatamente agora → tratado como expirado",
                            Optional.of(exactlyNowToken),
                            ResolveOutcome.DELETE_AND_CREATE,
                            true,
                            Duration.ofHours(24)
                    ),
                    new TokenResolveScenario(
                            "Sem token existente → cria novo com TTL de 24h",
                            Optional.empty(),
                            ResolveOutcome.CREATE,
                            false,
                            Duration.ofHours(24)
                    )
            );
        }
    }

}
