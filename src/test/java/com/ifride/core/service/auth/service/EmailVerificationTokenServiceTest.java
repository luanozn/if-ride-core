package com.ifride.core.service.auth.service;

import com.ifride.core.auth.model.TooManyRequestsException;
import com.ifride.core.auth.model.entity.ActionToken;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.TokenType;
import com.ifride.core.auth.repository.ActionTokenRepository;
import com.ifride.core.auth.service.EmailVerificationTokenService;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.events.emails.model.EmailVerificationEvent;
import com.ifride.core.shared.exceptions.InvalidTokenTypeException;
import com.ifride.core.shared.exceptions.TokenExpiredException;
import com.ifride.core.shared.exceptions.TokenNotFoundException;
import com.ifride.core.shared.rate_limit.RateLimitService;
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
import org.springframework.context.ApplicationEventPublisher;
import org.threeten.extra.MutableClock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EmailVerificationTokenServiceTest {

    @Mock
    private ActionTokenRepository tokenRepository;
    @Mock
    private UserService userService;
    @Mock
    private HashingService hashingService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private MutableClock clock;
    private RateLimitService rateLimitService;
    private EmailVerificationTokenService underTest;

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @BeforeEach
    void setUp() {
        clock = MutableClock.of(NOW, ZoneOffset.UTC);
        rateLimitService = new RateLimitService(clock);
        underTest = new EmailVerificationTokenService(
                tokenRepository,
                userService,
                hashingService,
                clock,
                rateLimitService,
                eventPublisher
        );
    }

    private static User user() {
        var user = new User();
        user.setId("user-1");
        user.setEmail("john.doe@email.com");
        user.setPassword("superSecretPassword");
        user.setName("John Doe");
        user.setCpfEncrypted("057.772.470-32");
        user.setEmailVerified(false);
        return user;
    }

    private ActionToken tokenExpiringAt(String tokenId, Instant instant, TokenType type, User user) {
        return new ActionToken(tokenId, instant, type, user);
    }

    @Nested
    @DisplayName("Geração e Ciclo de Vida dos Tokens")
    class TokenLifeCycleTests {

        @Test
        @DisplayName("Deve gerar novo token com TTL de 24h, salvar o hash e publicar EmailVerificationEvent")
        void shouldGenerateNewTokenAndPublishEvent() {
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
            assertThat(savedToken.getType()).isEqualTo(TokenType.EMAIL_VERIFICATION);
            assertThat(savedToken.getExpires()).isEqualTo(NOW.plus(Duration.ofHours(24)));
            assertThat(savedToken.getUser()).isEqualTo(user);

            var hashInputCaptor = ArgumentCaptor.forClass(String.class);
            verify(hashingService).hash(hashInputCaptor.capture());
            String plainToken = hashInputCaptor.getValue();

            var eventCaptor = ArgumentCaptor.forClass(EmailVerificationEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().email()).isEqualTo(user.getEmail());
            assertThat(eventCaptor.getValue().plainToken()).isEqualTo(plainToken);
        }

        @Test
        @DisplayName("Deve deletar o token existente e gerar um novo (Token Rotation)")
        void shouldDeleteExistingTokenAndGenerateNewOne() {
            var user = user();
            var existingToken = tokenExpiringAt("old-hash", NOW.plus(Duration.ofHours(1)), TokenType.EMAIL_VERIFICATION, user);

            given(tokenRepository.findByUserAndType(user, TokenType.EMAIL_VERIFICATION))
                    .willReturn(Optional.of(existingToken));
            given(hashingService.hash(anyString())).willReturn("new-hashed-token");

            underTest.generateTokenAndSendEmail(user);

            verify(tokenRepository).delete(existingToken);

            var tokenCaptor = ArgumentCaptor.forClass(ActionToken.class);
            verify(tokenRepository).save(tokenCaptor.capture());

            assertThat(tokenCaptor.getValue().getToken()).isEqualTo("new-hashed-token");
            assertThat(tokenCaptor.getValue().getExpires()).isEqualTo(NOW.plus(Duration.ofHours(24)));
            verify(eventPublisher).publishEvent(any(EmailVerificationEvent.class));
        }

        @Test
        @DisplayName("Não deve gerar token nem publicar evento se o e-mail já estiver verificado")
        void shouldDoNothingWhenEmailIsAlreadyVerified() {
            var user = user();
            user.setEmailVerified(true);

            underTest.generateTokenAndSendEmail(user);

            verifyNoInteractions(tokenRepository, hashingService, eventPublisher);
        }
    }

    @Nested
    @DisplayName("Rate limit de reenvio de e-mail")
    class RateLimitTests {

        @Test
        @DisplayName("Deve lançar TooManyRequestsException após 3 tentativas e não salvar ou publicar na 4ª")
        void shouldThrowAfterMaxAttempts() {
            var user = user();
            lenient().when(tokenRepository.findByUserAndType(user, TokenType.EMAIL_VERIFICATION))
                    .thenReturn(Optional.empty());
            lenient().when(hashingService.hash(anyString())).thenReturn("hashed");

            underTest.generateTokenAndSendEmail(user);
            underTest.generateTokenAndSendEmail(user);
            underTest.generateTokenAndSendEmail(user);

            assertThatThrownBy(() -> underTest.generateTokenAndSendEmail(user))
                    .isInstanceOf(TooManyRequestsException.class);

            verify(tokenRepository, times(3)).save(any());
            verify(eventPublisher, times(3)).publishEvent(any(EmailVerificationEvent.class));
        }

        @Test
        @DisplayName("Deve liberar novas tentativas após a janela de 15 minutos expirar")
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

            verify(tokenRepository, times(4)).save(any());
            verify(eventPublisher, times(4)).publishEvent(any(EmailVerificationEvent.class));
        }
    }

    @Nested
    @DisplayName("Confirmação de E-mail")
    class ConfirmationTests {

        @Test
        @DisplayName("Deve validar o hash, marcar e-mail como verificado e deletar o token")
        void shouldConfirmEmailSuccessfully() {
            var user = user();
            var plainToken = "plain-token-uuid";
            var hashedToken = "hashed-token-uuid";
            var validTokenEntity = tokenExpiringAt(hashedToken, NOW.plus(Duration.ofHours(1)), TokenType.EMAIL_VERIFICATION, user);

            given(hashingService.hash(plainToken)).willReturn(hashedToken);
            given(tokenRepository.findById(hashedToken)).willReturn(Optional.of(validTokenEntity));

            underTest.confirmEmailVerification(plainToken);

            assertThat(user.isEmailVerified()).isTrue();
            verify(userService).save(user);
            verify(tokenRepository).delete(validTokenEntity);
        }

        @Test
        @DisplayName("Deve lançar TokenNotFoundException se o token não existir no banco")
        void shouldThrowIfTokenNotFound() {
            var plainToken = "plain-token-uuid";
            given(hashingService.hash(plainToken)).willReturn("non-existent-hash");
            given(tokenRepository.findById("non-existent-hash")).willReturn(Optional.empty());

            assertThatThrownBy(() -> underTest.confirmEmailVerification(plainToken))
                    .isInstanceOf(TokenNotFoundException.class);

            verify(userService, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar InvalidTokenTypeException se o token for de troca de senha")
        void shouldThrowIfTokenTypeIsInvalid() {
            var user = user();
            var plainToken = "password-change-plain-token";
            var wrongTypeToken = tokenExpiringAt("hash", NOW.plus(Duration.ofMinutes(10)), TokenType.PASSWORD_CHANGE, user);

            given(hashingService.hash(plainToken)).willReturn("hash");
            given(tokenRepository.findById("hash")).willReturn(Optional.of(wrongTypeToken));

            assertThatThrownBy(() -> underTest.confirmEmailVerification(plainToken))
                    .isInstanceOf(InvalidTokenTypeException.class);

            assertThat(user.isEmailVerified()).isFalse();
            verify(userService, never()).save(any());
            verify(tokenRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Deve lançar TokenExpiredException se o token estiver expirado")
        void shouldThrowIfTokenExpired() {
            var user = user();
            var plainToken = "plain-token-uuid";
            var expiredTokenEntity = tokenExpiringAt("hash", NOW.minus(Duration.ofHours(1)), TokenType.EMAIL_VERIFICATION, user);

            given(hashingService.hash(plainToken)).willReturn("hash");
            given(tokenRepository.findById("hash")).willReturn(Optional.of(expiredTokenEntity));

            assertThatThrownBy(() -> underTest.confirmEmailVerification(plainToken))
                    .isInstanceOf(TokenExpiredException.class);

            assertThat(user.isEmailVerified()).isFalse();
            verify(userService, never()).save(any());
            verify(tokenRepository, never()).delete(any());
        }
    }
}