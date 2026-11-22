package com.ifride.core.service.auth.service;

import com.ifride.core.auth.model.TooManyRequestsException;
import com.ifride.core.auth.model.entity.ActionToken;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.TokenType;
import com.ifride.core.auth.repository.ActionTokenRepository;
import com.ifride.core.auth.service.ChangePasswordTokenService;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.events.emails.model.ChangePasswordRequestedEvent;
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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.threeten.extra.MutableClock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.lenient;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChangePasswordTokenServiceTest {

    @Mock
    private ActionTokenRepository tokenRepository;
    @Mock
    private UserService userService;
    @Mock
    private HashingService hashingService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private MutableClock clock;
    private ChangePasswordTokenService underTest;

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @BeforeEach
    void setUp() {
        clock = MutableClock.of(NOW, ZoneOffset.UTC);
        RateLimitService rateLimitService = new RateLimitService(clock);
        underTest = new ChangePasswordTokenService(
                tokenRepository,
                userService,
                hashingService,
                clock,
                passwordEncoder,
                rateLimitService,
                eventPublisher
        );
    }

    private static User user() {
        var user = new User();
        user.setId("user-1");
        user.setEmail("john.doe@email.com");
        user.setPassword("oldHashedPassword");
        user.setName("John Doe");
        return user;
    }

    private ActionToken tokenExpiringAt(String tokenId, Instant instant, TokenType type, User user) {
        return new ActionToken(tokenId, instant, type, user);
    }

    @Nested
    @DisplayName("Geração e Ciclo de Vida dos Tokens")
    class TokenLifeCycleTests {

        @Test
        @DisplayName("Deve gerar novo token com TTL de 15min, salvar o hash e publicar ChangePasswordRequestedEvent")
        void shouldGenerateNewTokenAndPublishEvent() {
            var user = user();
            given(tokenRepository.findByUserAndType(user, TokenType.PASSWORD_CHANGE))
                    .willReturn(Optional.empty());
            given(hashingService.hash(anyString())).willReturn("hashed-reset-token");

            underTest.generateTokenAndSendEmail(user);

            verify(tokenRepository, never()).delete(any());

            var tokenCaptor = ArgumentCaptor.forClass(ActionToken.class);
            verify(tokenRepository).save(tokenCaptor.capture());

            var savedToken = tokenCaptor.getValue();
            assertThat(savedToken.getToken()).isEqualTo("hashed-reset-token");
            assertThat(savedToken.getType()).isEqualTo(TokenType.PASSWORD_CHANGE);
            assertThat(savedToken.getExpires()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
            assertThat(savedToken.getUser()).isEqualTo(user);

            var hashInputCaptor = ArgumentCaptor.forClass(String.class);
            verify(hashingService).hash(hashInputCaptor.capture());
            String plainToken = hashInputCaptor.getValue();

            var eventCaptor = ArgumentCaptor.forClass(ChangePasswordRequestedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().email()).isEqualTo(user.getEmail());
            assertThat(eventCaptor.getValue().plainToken()).isEqualTo(plainToken);
        }

        @Test
        @DisplayName("Deve deletar o token anterior e gerar um novo (Token Rotation)")
        void shouldDeleteExistingTokenAndGenerateNewOne() {
            var user = user();
            var existingToken = tokenExpiringAt("old-hash", NOW.plus(Duration.ofMinutes(5)), TokenType.PASSWORD_CHANGE, user);

            given(tokenRepository.findByUserAndType(user, TokenType.PASSWORD_CHANGE))
                    .willReturn(Optional.of(existingToken));
            given(hashingService.hash(anyString())).willReturn("new-hashed-reset-token");

            underTest.generateTokenAndSendEmail(user);

            verify(tokenRepository).delete(existingToken);

            var tokenCaptor = ArgumentCaptor.forClass(ActionToken.class);
            verify(tokenRepository).save(tokenCaptor.capture());

            assertThat(tokenCaptor.getValue().getToken()).isEqualTo("new-hashed-reset-token");
            assertThat(tokenCaptor.getValue().getExpires()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
            verify(eventPublisher).publishEvent(any(ChangePasswordRequestedEvent.class));
        }
    }

    @Nested
    @DisplayName("Rate limit de solicitação de redefinição")
    class RateLimitTests {

        @Test
        @DisplayName("Deve lançar TooManyRequestsException após 3 tentativas e não salvar ou publicar na 4ª")
        void shouldThrowAfterMaxAttempts() {
            var user = user();
            lenient().when(tokenRepository.findByUserAndType(user, TokenType.PASSWORD_CHANGE))
                    .thenReturn(Optional.empty());
            lenient().when(hashingService.hash(anyString())).thenReturn("hashed");

            underTest.generateTokenAndSendEmail(user);
            underTest.generateTokenAndSendEmail(user);
            underTest.generateTokenAndSendEmail(user);

            assertThatThrownBy(() -> underTest.generateTokenAndSendEmail(user))
                    .isInstanceOf(TooManyRequestsException.class);

            verify(tokenRepository, times(3)).save(any());
            verify(eventPublisher, times(3)).publishEvent(any(ChangePasswordRequestedEvent.class));
        }

        @Test
        @DisplayName("Deve liberar novas tentativas após a janela de 15 minutos expirar")
        void shouldResetAfterWindow() {
            var user = user();
            lenient().when(tokenRepository.findByUserAndType(user, TokenType.PASSWORD_CHANGE))
                    .thenReturn(Optional.empty());
            lenient().when(hashingService.hash(anyString())).thenReturn("hashed");

            underTest.generateTokenAndSendEmail(user);
            underTest.generateTokenAndSendEmail(user);
            underTest.generateTokenAndSendEmail(user);

            clock.add(Duration.ofMinutes(16));

            underTest.generateTokenAndSendEmail(user);

            verify(tokenRepository, times(4)).save(any());
            verify(eventPublisher, times(4)).publishEvent(any(ChangePasswordRequestedEvent.class));
        }
    }

    @Nested
    @DisplayName("Conclusão da Troca de Senha")
    class ResetPasswordTests {

        @Test
        @DisplayName("Deve validar o token, codificar a nova senha, salvar o usuário e deletar o token")
        void shouldResetPasswordAndDeleteToken() {
            var user = user();
            var plainToken = "plain-reset-token";
            var hashedToken = "hashed-reset-token";
            var validTokenEntity = tokenExpiringAt(hashedToken, NOW.plus(Duration.ofMinutes(10)), TokenType.PASSWORD_CHANGE, user);

            given(hashingService.hash(plainToken)).willReturn(hashedToken);
            given(tokenRepository.findById(hashedToken)).willReturn(Optional.of(validTokenEntity));
            given(passwordEncoder.encode("novaSenhaForte123")).willReturn("encodedNewPassword");

            underTest.resetPassword(plainToken, "novaSenhaForte123");

            assertThat(user.getPassword()).isEqualTo("encodedNewPassword");

            InOrder inOrder = inOrder(passwordEncoder, userService, tokenRepository);
            inOrder.verify(passwordEncoder).encode("novaSenhaForte123");
            inOrder.verify(userService).save(user);
            inOrder.verify(tokenRepository).delete(validTokenEntity);
        }

        @Test
        @DisplayName("Deve lançar TokenNotFoundException se o token não for encontrado")
        void shouldThrowIfTokenNotFound() {
            given(hashingService.hash("token-inexistente")).willReturn("missing-hash");
            given(tokenRepository.findById("missing-hash")).willReturn(Optional.empty());

            assertThatThrownBy(() -> underTest.resetPassword("token-inexistente", "novaSenha123"))
                    .isInstanceOf(TokenNotFoundException.class);

            verify(passwordEncoder, never()).encode(any());
            verify(userService, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar InvalidTokenTypeException se o token for de verificação de e-mail")
        void shouldThrowIfTokenTypeIsInvalid() {
            var user = user();
            var plainToken = "email-verification-token";
            var wrongTypeToken = tokenExpiringAt("hash", NOW.plus(Duration.ofHours(1)), TokenType.EMAIL_VERIFICATION, user);

            given(hashingService.hash(plainToken)).willReturn("hash");
            given(tokenRepository.findById("hash")).willReturn(Optional.of(wrongTypeToken));

            assertThatThrownBy(() -> underTest.resetPassword(plainToken, "novaSenha123"))
                    .isInstanceOf(InvalidTokenTypeException.class);

            verify(passwordEncoder, never()).encode(any());
            verify(userService, never()).save(any());
            verify(tokenRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Deve lançar TokenExpiredException se o token estiver expirado")
        void shouldThrowIfTokenExpired() {
            var user = user();
            var plainToken = "expired-reset-token";
            var expiredToken = tokenExpiringAt("hash", NOW.minus(Duration.ofMinutes(1)), TokenType.PASSWORD_CHANGE, user);

            given(hashingService.hash(plainToken)).willReturn("hash");
            given(tokenRepository.findById("hash")).willReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> underTest.resetPassword(plainToken, "novaSenha123"))
                    .isInstanceOf(TokenExpiredException.class);

            verify(passwordEncoder, never()).encode(any());
            verify(userService, never()).save(any());
            verify(tokenRepository, never()).delete(any());
        }
    }
}