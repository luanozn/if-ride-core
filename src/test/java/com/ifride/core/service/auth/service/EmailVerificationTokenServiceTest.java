package com.ifride.core.service.auth.service;

import com.ifride.core.auth.model.EmailVerificationMaxAttemptsExceededException;
import com.ifride.core.auth.model.entity.ActionToken;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.TokenType;
import com.ifride.core.auth.repository.ActionTokenRepository;
import com.ifride.core.auth.service.EmailVerificationTokenService;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.service.auth.utils.TokenResolveScenario;
import com.ifride.core.service.auth.utils.TokenResolveScenario.ResolveOutcome;
import com.ifride.core.shared.services.EmailService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.threeten.extra.MutableClock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class EmailVerificationTokenServiceTest {

    @Mock
    ActionTokenRepository tokenRepository;
    @Mock
    UserService userService;
    @Mock
    EmailService emailService;

    Clock clock;
    EmailVerificationTokenService underTest;

    static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @BeforeEach
    void setUp() {
        clock = MutableClock.of(NOW, ZoneOffset.UTC);
        underTest = new EmailVerificationTokenService(tokenRepository, userService, emailService, clock);
    }

    private static User user() {
        var user = new User();
        user.setId("user-1");
        user.setEmail("john.doe@email.com");
        user.setPassword("superSecretPassword");
        user.setName("John Doe");
        user.setCpfEncrypted("057.772.470-32");
        return user;
    }

    public static ActionToken tokenExpiringAt(Instant instant, User user) {
        return new ActionToken(instant, TokenType.EMAIL_VERIFICATION, user);
    }

    @Nested()
    @DisplayName("Ciclo de vida dos tokens")
    class TokenLifeCycleTests {

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("tokenScenarios")
        void shouldResolveTokenCorrectly(TokenResolveScenario scenario) {

            given(tokenRepository.findByUserAndType(user(), TokenType.EMAIL_VERIFICATION))
                    .willReturn(scenario.existingToken());

            underTest.generateTokenAndSendEmail(user());

            if (scenario.shouldDelete()) {
                verify(tokenRepository).delete(scenario.existingToken().orElseThrow());
            } else {
                verify(tokenRepository, never()).delete(any());
            }

            var savedCaptor = ArgumentCaptor.forClass(ActionToken.class);
            verify(tokenRepository).save(savedCaptor.capture());

            assertThat(savedCaptor.getValue().getExpires())
                    .as("TTL esperado para o cenário: %s", scenario.description())
                    .isEqualTo(NOW.plus(scenario.expectedTtl()));
        }

        static Stream<TokenResolveScenario> tokenScenarios() {
            var futureToken = tokenExpiringAt(NOW.plus(Duration.ofHours(1)), user());
            var expiredToken = tokenExpiringAt(NOW.minus(Duration.ofHours(1)), user());
            var exactlyNowToken = tokenExpiringAt(NOW, user());

            return Stream.of(
                    new TokenResolveScenario(
                            "Token válido no futuro → reusa sem deletar",
                            Optional.of(futureToken),
                            ResolveOutcome.REUSE,
                            false,
                            Duration.ofHours(1)
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
                            ResolveOutcome.REUSE,
                            false,
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

    @Nested
    @DisplayName("Rate limit de reenvio de e-mail")
    class RateLimitTests {

        @Test
        void shouldThrowAfterMaxAttempts() {
            given(tokenRepository.findByUserAndType(user(), TokenType.EMAIL_VERIFICATION))
                    .willReturn(Optional.empty());

            underTest.generateTokenAndSendEmail(user());
            underTest.generateTokenAndSendEmail(user());
            underTest.generateTokenAndSendEmail(user());

            assertThatThrownBy(() -> underTest.generateTokenAndSendEmail(user()))
                    .isInstanceOf(EmailVerificationMaxAttemptsExceededException.class);
        }

        @Test
        void shouldResetAfterWindow() {
            given(tokenRepository.findByUserAndType(user(), TokenType.EMAIL_VERIFICATION))
                    .willReturn(Optional.empty());

            underTest.generateTokenAndSendEmail(user());
            underTest.generateTokenAndSendEmail(user());
            underTest.generateTokenAndSendEmail(user());

            ((MutableClock) clock).add(Duration.ofMinutes(16));

            underTest.generateTokenAndSendEmail(user());
        }
    }

}
