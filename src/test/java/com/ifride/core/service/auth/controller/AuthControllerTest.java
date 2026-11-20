package com.ifride.core.service.auth.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ifride.core.auth.controller.AuthController;
import com.ifride.core.auth.model.dto.LoginRequestDTO;
import com.ifride.core.auth.model.dto.LoginResponseDTO;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.service.AuthService;
import com.ifride.core.auth.service.JwtService;
import com.ifride.core.auth.service.LoginRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private AuthService authService;
    @Mock private LoginRateLimitService rateLimitService;
    @Mock private Authentication authentication;

    @InjectMocks private AuthController controller;

    private User user;
    private LoginRequestDTO credentials;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId("user-1");
        credentials = new LoginRequestDTO("user@example.com", "secret");
        request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");
    }

    @Test
    @DisplayName("Checa o rate limit antes de autenticar e limpa o contador após sucesso")
    void successfulLoginChecksLimitAndResetsEmailCounter() {
        LoginResponseDTO expected = new LoginResponseDTO("jwt", "user@example.com", 123L, "user-1", "Pessoa");
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        when(jwtService.generateLoginResponse(user)).thenReturn(expected);

        LoginResponseDTO actual = controller.login(credentials, request);

        assertSame(expected, actual);
        verify(rateLimitService).checkBlocked("user@example.com", "192.0.2.10");
        verify(rateLimitService).loginSucceeded("user@example.com");
        verify(rateLimitService, never()).loginFailed(any(), any());
    }

    @Test
    @DisplayName("Registra falha de autenticação para e-mail e IP, e repassa a exceção")
    void failedLoginRegistersAttemptAndRethrowsAuthenticationException() {
        BadCredentialsException failure = new BadCredentialsException("Credenciais inválidas");
        when(authenticationManager.authenticate(any())).thenThrow(failure);

        BadCredentialsException thrown = assertThrows(BadCredentialsException.class,
                () -> controller.login(credentials, request));

        assertSame(failure, thrown);
        verify(rateLimitService).loginFailed("user@example.com", "192.0.2.10");
        verify(rateLimitService, never()).loginSucceeded(any());
    }

    @Test
    @DisplayName("Não tenta autenticar quando o rate limit já bloqueou a requisição")
    void blockedLoginDoesNotAuthenticate() {
        when(rateLimitService.checkBlocked("user@example.com", "192.0.2.10"))
                .thenThrow(new com.ifride.core.auth.model.TooManyRequestsException("Bloqueado"));

        assertThrows(com.ifride.core.auth.model.TooManyRequestsException.class,
                () -> controller.login(credentials, request));

        verify(authenticationManager, never()).authenticate(any());
        verify(rateLimitService, never()).loginFailed(any(), any());
    }

    @Test
    @DisplayName("Usa o primeiro IP encaminhado pelo proxy")
    void usesClientIpFromForwardedHeader() {
        request.addHeader("X-Forwarded-For", "198.51.100.7, 10.0.0.1");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("inválido"));

        assertThrows(BadCredentialsException.class, () -> controller.login(credentials, (HttpServletRequest) request));

        verify(rateLimitService).checkBlocked("user@example.com", "198.51.100.7");
    }
}
