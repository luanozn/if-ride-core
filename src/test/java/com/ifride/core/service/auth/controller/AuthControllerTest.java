package com.ifride.core.service.auth.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.ifride.core.auth.controller.AuthController;
import com.ifride.core.auth.model.TooManyRequestsException;
import com.ifride.core.auth.model.dto.ForgotPasswordDTO;
import com.ifride.core.auth.model.dto.LoginRequestDTO;
import com.ifride.core.auth.model.dto.LoginResponseDTO;
import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.auth.model.dto.RegisterResponseDTO;
import com.ifride.core.auth.model.dto.RequestPasswordChangeDTO;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.service.AuthService;
import com.ifride.core.auth.service.ChangePasswordTokenService;
import com.ifride.core.auth.service.JwtService;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.shared.exceptions.TokenExpiredException;
import com.ifride.core.shared.exceptions.TokenNotFoundException;
import com.ifride.core.shared.rate_limit.RateLimitOperation;
import com.ifride.core.shared.rate_limit.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private AuthService authService;
    @Mock private RateLimitService rateLimitService;
    @Mock private ChangePasswordTokenService changePasswordTokenService;
    @Mock private UserService userService;
    @Mock private Authentication authentication;

    @InjectMocks private AuthController controller;

    private User user;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId("user-1");
        user.setEmail("user@example.com");
        user.setName("Pessoa");

        request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");
    }

    @Nested
    @DisplayName("POST /v1/auth/login")
    class LoginTests {

        private final LoginRequestDTO credentials = new LoginRequestDTO("user@example.com", "secret");

        @Test
        @DisplayName("Checa os limites de e-mail e IP antes de autenticar e reseta o limite do e-mail após sucesso")
        void successfulLoginChecksLimitsAndResetsEmailCounter() {
            LoginResponseDTO expected = new LoginResponseDTO("jwt", "user@example.com", 123L, "user-1", "Pessoa");
            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(user);
            when(jwtService.generateLoginResponse(user)).thenReturn(expected);

            LoginResponseDTO actual = controller.login(credentials, request);

            assertSame(expected, actual);

            InOrder inOrder = inOrder(rateLimitService, authenticationManager, jwtService);
            inOrder.verify(rateLimitService).checkLimit(RateLimitOperation.LOGIN_EMAIL, "user@example.com", 5, Duration.ofMinutes(15));
            inOrder.verify(rateLimitService).checkLimit(RateLimitOperation.LOGIN_IP, "192.0.2.10", 50, Duration.ofMinutes(15));
            inOrder.verify(authenticationManager).authenticate(any());
            inOrder.verify(rateLimitService).resetLimit(RateLimitOperation.LOGIN_EMAIL, "user@example.com");
            inOrder.verify(jwtService).generateLoginResponse(user);
        }

        @Test
        @DisplayName("Mantém o consumo do rate limit e repassa BadCredentialsException quando a autenticação falha")
        void failedLoginKeepsLimitConsumptionAndRethrowsException() {
            BadCredentialsException failure = new BadCredentialsException("Credenciais inválidas");
            when(authenticationManager.authenticate(any())).thenThrow(failure);

            BadCredentialsException thrown = assertThrows(
                    BadCredentialsException.class,
                    () -> controller.login(credentials, request)
            );

            assertSame(failure, thrown);
            verify(rateLimitService).checkLimit(RateLimitOperation.LOGIN_EMAIL, "user@example.com", 5, Duration.ofMinutes(15));
            verify(rateLimitService).checkLimit(RateLimitOperation.LOGIN_IP, "192.0.2.10", 50, Duration.ofMinutes(15));
            verify(rateLimitService, never()).resetLimit(any(), any());
            verify(jwtService, never()).generateLoginResponse(any());
        }

        @Test
        @DisplayName("Não tenta autenticar quando o limite por e-mail já foi excedido")
        void blockedByEmailDoesNotAuthenticate() {
            doThrow(new TooManyRequestsException("Bloqueado"))
                    .when(rateLimitService)
                    .checkLimit(eq(RateLimitOperation.LOGIN_EMAIL), eq("user@example.com"), eq(5), any());

            assertThrows(TooManyRequestsException.class, () -> controller.login(credentials, request));

            verify(authenticationManager, never()).authenticate(any());
            verify(rateLimitService, never()).resetLimit(any(), any());
        }

        @Test
        @DisplayName("Não tenta autenticar quando o limite por IP já foi excedido")
        void blockedByIpDoesNotAuthenticate() {
            lenient().doThrow(new TooManyRequestsException("IP bloqueado"))
                    .when(rateLimitService)
                    .checkLimit(eq(RateLimitOperation.LOGIN_IP), eq("192.0.2.10"), eq(50), any());

            assertThrows(TooManyRequestsException.class, () -> controller.login(credentials, request));

            verify(authenticationManager, never()).authenticate(any());
            verify(rateLimitService, never()).resetLimit(any(), any());
        }

        @Test
        @DisplayName("Extrai o primeiro IP do cabeçalho X-Forwarded-For")
        void usesFirstClientIpFromForwardedHeader() {
            request.addHeader("X-Forwarded-For", "198.51.100.7, 10.0.0.1");
            when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("inválido"));

            assertThrows(BadCredentialsException.class, () -> controller.login(credentials, (HttpServletRequest) request));

            verify(rateLimitService).checkLimit(RateLimitOperation.LOGIN_IP, "198.51.100.7", 50, Duration.ofMinutes(15));
        }
    }

    @Nested
    @DisplayName("POST /v1/auth/register")
    class RegisterTests {

        private final RegisterRequestDTO registerDTO = new RegisterRequestDTO(
                "novo@example.com", "senhaSegura123", "Novo Usuário", "053.440.630-08", LocalDate.of(1998, 3, 10)
        );

        @Test
        @DisplayName("Valida rate limit de registro por IP e cria o usuário com sucesso")
        void successfulRegistrationChecksIpLimitAndCallsService() {
            User savedUser = new User();
            savedUser.setId("user-2");
            savedUser.setEmail("novo@example.com");
            savedUser.setName("Novo Usuário");

            when(authService.register(registerDTO)).thenReturn(savedUser);

            RegisterResponseDTO response = controller.register(registerDTO, request);

            assertEquals("novo@example.com", response.email());
            verify(rateLimitService).checkLimit(RateLimitOperation.REGISTRATION_IP, "192.0.2.10", 100, Duration.ofHours(1));
            verify(authService).register(registerDTO);
        }

        @Test
        @DisplayName("Bloqueia o registro quando o IP excede o limite horário")
        void blockedRegistrationDoesNotCallAuthService() {
            doThrow(new TooManyRequestsException("Muitos registros"))
                    .when(rateLimitService)
                    .checkLimit(eq(RateLimitOperation.REGISTRATION_IP), eq("192.0.2.10"), eq(100), eq(Duration.ofHours(1)));

            assertThrows(TooManyRequestsException.class, () -> controller.register(registerDTO, request));

            verify(authService, never()).register(any());
        }
    }

    @Nested
    @DisplayName("POST /v1/auth/forgot-password")
    class ForgotPasswordTests {

        private final ForgotPasswordDTO dto = new ForgotPasswordDTO("user@example.com");

        @Test
        @DisplayName("Gera token e envia e-mail quando o usuário existe")
        void sendsResetEmailWhenUserExists() {
            when(userService.findByEmail("user@example.com")).thenReturn(user);

            ResponseEntity<Void> response = controller.forgotPassword(dto, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(rateLimitService).checkLimit(RateLimitOperation.FORGOT_PASSWORD_IP, "192.0.2.10", 50, Duration.ofMinutes(15));
            verify(changePasswordTokenService).generateTokenAndSendEmail(user);
        }

        @Test
        @DisplayName("Retorna 200 OK mesmo quando o e-mail não existe para evitar enumeração de usuários")
        void returnsOkWhenEmailDoesNotExist() {
            when(userService.findByEmail("user@example.com")).thenThrow(new RuntimeException("Usuário não encontrado"));

            ResponseEntity<Void> response = controller.forgotPassword(dto, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(changePasswordTokenService, never()).generateTokenAndSendEmail(any());
        }

        @Test
        @DisplayName("Retorna 200 OK mesmo quando o rate limit individual do usuário é excedido")
        void returnsOkWhenUserTokenRateLimitExceeded() {
            when(userService.findByEmail("user@example.com")).thenReturn(user);
            doThrow(new TooManyRequestsException("Limite por usuário"))
                    .when(changePasswordTokenService)
                    .generateTokenAndSendEmail(user);

            ResponseEntity<Void> response = controller.forgotPassword(dto, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Retorna 429 (lança TooManyRequestsException) quando o IP excede o limite de solicitações")
        void throwsTooManyRequestsWhenIpLimitExceeded() {
            doThrow(new TooManyRequestsException("IP bloqueado"))
                    .when(rateLimitService)
                    .checkLimit(eq(RateLimitOperation.FORGOT_PASSWORD_IP), eq("192.0.2.10"), eq(50), any());

            assertThrows(TooManyRequestsException.class, () -> controller.forgotPassword(dto, request));

            verify(userService, never()).findByEmail(any());
            verify(changePasswordTokenService, never()).generateTokenAndSendEmail(any());
        }
    }

    @Nested
    @DisplayName("POST /v1/auth/reset-password")
    class ResetPasswordTests {

        @Test
        @DisplayName("Redefine a senha com sucesso quando o token é válido")
        void resetsPasswordSuccessfully() {
            var dto = new RequestPasswordChangeDTO("plain-token-123", "novaSenhaSegura123");

            ResponseEntity<Void> response = controller.resetPassword(dto);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(changePasswordTokenService).resetPassword("plain-token-123", "novaSenhaSegura123");
        }

        @Test
        @DisplayName("Propaga exceção quando o token não existe ou já foi utilizado")
        void propagatesTokenNotFoundException() {
            var dto = new RequestPasswordChangeDTO("token-invalido", "novaSenhaSegura123");
            doThrow(new TokenNotFoundException("Token não encontrado"))
                    .when(changePasswordTokenService)
                    .resetPassword("token-invalido", "novaSenhaSegura123");

            assertThrows(TokenNotFoundException.class, () -> controller.resetPassword(dto));
        }

        @Test
        @DisplayName("Propaga exceção quando o token está expirado")
        void propagatesTokenExpiredException() {
            var dto = new RequestPasswordChangeDTO("token-expirado", "novaSenhaSegura123");
            doThrow(new TokenExpiredException("Token expirado"))
                    .when(changePasswordTokenService)
                    .resetPassword("token-expirado", "novaSenhaSegura123");

            assertThrows(TokenExpiredException.class, () -> controller.resetPassword(dto));
        }
    }
}