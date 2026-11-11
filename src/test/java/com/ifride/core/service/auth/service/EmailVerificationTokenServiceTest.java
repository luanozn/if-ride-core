package com.ifride.core.service.auth.service;

import com.ifride.core.auth.model.EmailVerificationMaxAttemptsExceededException;
import com.ifride.core.auth.model.entity.ActionToken;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.TokenType;
import com.ifride.core.auth.repository.ActionTokenRepository;
import com.ifride.core.auth.service.EmailVerificationTokenService;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.shared.exceptions.TokenExpiredException;
import com.ifride.core.shared.exceptions.TokenNotFoundException;
import com.ifride.core.shared.services.EmailService;
import com.ifride.core.shared.services.HashingService;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.threeten.extra.MutableClock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.lenient;
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
    @Mock
    HashingService hashingService;

    MutableClock clock;
    EmailVerificationTokenService underTest;

    static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @BeforeEach
    void setUp() {
        clock = MutableClock.of(NOW, ZoneOffset.UTC);
        underTest = new EmailVerificationTokenService(tokenRepository, userService, emailService, hashingService, clock);
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

    // Helper atualizado para o novo construtor do ActionToken
    private ActionToken tokenExpiringAt(String tokenId, Instant instant, User user) {
        return new ActionToken(tokenId, instant, TokenType.EMAIL_VERIFICATION, user);
    }

    @Nested
    @DisplayName("Geração e Ciclo de Vida dos Tokens")
    class TokenLifeCycleTests {

        @Test
        @DisplayName("Deve gerar novo token, salvar o hash e enviar texto plano no email")
        void shouldGenerateNewTokenAndSendEmail() {
            var user = user();
            given(tokenRepository.findByUserAndType(user, TokenType.EMAIL_VERIFICATION))
                    .willReturn(Optional.empty());
            given(hashingService.hash(anyString())).willReturn("hashed-token-uuid");

            underTest.generateTokenAndSendEmail(user);

            verify(tokenRepository, never()).delete(any());

            var tokenCaptor = ArgumentCaptor.forClass(ActionToken.class);
            verify(tokenRepository).save(tokenCaptor.capture());

            var savedToken = tokenCaptor.getValue();
            assertThat(savedToken.getToken()).isEqualTo("hashed-token-uuid");
            assertThat(savedToken.getExpires()).isEqualTo(NOW.plus(Duration.ofHours(24)));
            assertThat(savedToken.getUser()).isEqualTo(user);

            var hashInputCaptor = ArgumentCaptor.forClass(String.class);
            verify(hashingService).hash(hashInputCaptor.capture());
            String plainToken = hashInputCaptor.getValue();

            verify(emailService).sendEmailVerificationEmail(user.getEmail(), plainToken);
        }

        @Test
        @DisplayName("Deve deletar o token existente e gerar um novo (Token Rotation)")
        void shouldDeleteExistingTokenAndGenerateNewOne() {
            var user = user();
            var existingToken = tokenExpiringAt("old-hash", NOW.plus(Duration.ofHours(1)), user);

            given(tokenRepository.findByUserAndType(user, TokenType.EMAIL_VERIFICATION))
                    .willReturn(Optional.of(existingToken));
            given(hashingService.hash(anyString())).willReturn("new-hashed-token");

            underTest.generateTokenAndSendEmail(user);

            verify(tokenRepository).delete(existingToken);

            var tokenCaptor = ArgumentCaptor.forClass(ActionToken.class);
            verify(tokenRepository).save(tokenCaptor.capture());

            assertThat(tokenCaptor.getValue().getToken()).isEqualTo("new-hashed-token");
            assertThat(tokenCaptor.getValue().getExpires()).isEqualTo(NOW.plus(Duration.ofHours(24)));
        }
    }

    @Nested
    @DisplayName("Rate limit de reenvio de e-mail")
    class RateLimitTests {

        @Test
        void shouldThrowAfterMaxAttempts() {
            var user = user();
            lenient().when(tokenRepository.findByUserAndType(user, TokenType.EMAIL_VERIFICATION))
                    .thenReturn(Optional.empty());
            lenient().when(hashingService.hash(anyString())).thenReturn("hashed");

            underTest.generateTokenAndSendEmail(user);
            underTest.generateTokenAndSendEmail(user);
            underTest.generateTokenAndSendEmail(user);

            assertThatThrownBy(() -> underTest.generateTokenAndSendEmail(user))
                    .isInstanceOf(EmailVerificationMaxAttemptsExceededException.class);
        }

        @Test
        void shouldResetAfterWindow() {
            var user = user();
            lenient().when(tokenRepository.findByUserAndType(user, TokenType.EMAIL_VERIFICATION))
                    .thenReturn(Optional.empty());
            lenient().when(hashingService.hash(anyString())).thenReturn("hashed");

            underTest.generateTokenAndSendEmail(user);
            underTest.generateTokenAndSendEmail(user);
            underTest.generateTokenAndSendEmail(user);

            clock.add(Duration.ofMinutes(16));

            underTest.generateTokenAndSendEmail(user);
        }
    }

    @Nested
    @DisplayName("Confirmação de E-mail")
    class ConfirmationTests {

        @Test
        @DisplayName("Deve validar o hash e verificar o e-mail do usuário")
        void shouldConfirmEmailSuccessfully() {
            var user = user();
            var plainToken = "plain-token-uuid";
            var hashedToken = "hashed-token-uuid";
            var validTokenEntity = tokenExpiringAt(hashedToken, NOW.plus(Duration.ofHours(1)), user);

            given(hashingService.hash(plainToken)).willReturn(hashedToken);
            given(tokenRepository.findById(hashedToken)).willReturn(Optional.of(validTokenEntity));

            underTest.confirmEmailVerification(plainToken);

            assertThat(user.isEmailVerified()).isTrue();
            verify(userService).save(user);
            verify(tokenRepository).delete(validTokenEntity);
        }

        @Test
        @DisplayName("Deve lançar exceção se o token não for encontrado no banco")
        void shouldThrowIfTokenNotFound() {
            var plainToken = "plain-token-uuid";
            given(hashingService.hash(plainToken)).willReturn("non-existent-hash");
            given(tokenRepository.findById("non-existent-hash")).willReturn(Optional.empty());

            assertThatThrownBy(() -> underTest.confirmEmailVerification(plainToken))
                    .isInstanceOf(TokenNotFoundException.class);
        }

        @Test
        @DisplayName("Deve lançar exceção se o token estiver expirado")
        void shouldThrowIfTokenExpired() {
            var user = user();
            var plainToken = "plain-token-uuid";
            var expiredTokenEntity = tokenExpiringAt("hash", NOW.minus(Duration.ofHours(1)), user);

            given(hashingService.hash(plainToken)).willReturn("hash");
            given(tokenRepository.findById("hash")).willReturn(Optional.of(expiredTokenEntity));

            assertThatThrownBy(() -> underTest.confirmEmailVerification(plainToken))
                    .isInstanceOf(TokenExpiredException.class);
        }
    }
}