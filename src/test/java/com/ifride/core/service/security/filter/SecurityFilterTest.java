package com.ifride.core.service.security.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.repository.UserRepository;
import com.ifride.core.auth.service.JwtService;
import com.ifride.core.shared.config.security.filter.SecurityFilter;
import com.ifride.core.shared.exceptions.api.ForbiddenException;
import java.io.IOException;
import java.util.Optional;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class SecurityFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UserRepository userRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Ignora authorization que não usa o esquema Bearer")
    void ignoresMalformedAuthorizationHeader() throws ServletException, IOException {
        SecurityFilter filter = new SecurityFilter(jwtService, userRepository);
        MockHttpServletRequest request = request("Basic abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        verify(jwtService, never()).validateToken(org.mockito.ArgumentMatchers.anyString());
        assertNotNull(chain.getRequest());
    }

    @Test
    @DisplayName("Retorna 401 se o token for válido, mas a conta não existir")
    void returnsUnauthorizedWhenUserIsMissing() throws ServletException, IOException {
        SecurityFilter filter = new SecurityFilter(jwtService, userRepository);
        MockHttpServletRequest request = request("Bearer jwt-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtService.validateToken("jwt-token")).thenReturn("deleted@example.com");
        when(userRepository.findByEmail("deleted@example.com")).thenReturn(Optional.empty());

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
        assertEquals("UTF-8", response.getCharacterEncoding());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        org.junit.jupiter.api.Assertions.assertTrue(response.getContentAsString().contains("Usuário não encontrado ou conta excluída."));
    }

    @Test
    @DisplayName("Permite rota de perfil para usuário sem e-mail verificado")
    void allowsUnverifiedUserOnProfileRoute() throws ServletException, IOException {
        SecurityFilter filter = new SecurityFilter(jwtService, userRepository);
        MockHttpServletRequest request = request("Bearer jwt-token");
        request.setServletPath("/v1/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        User user = user(false);
        when(jwtService.validateToken("jwt-token")).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    @DisplayName("Bloqueia rota protegida para usuário sem e-mail verificado")
    void blocksUnverifiedUserOnProtectedRoute() throws ServletException, IOException {
        SecurityFilter filter = new SecurityFilter(jwtService, userRepository);
        MockHttpServletRequest request = request("Bearer jwt-token");
        request.setServletPath("/v1/rides");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtService.validateToken("jwt-token")).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user(false)));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertNotNull(response.getContentAsString());
        org.junit.jupiter.api.Assertions.assertNull(chain.getRequest());
    }

    @Test
    @DisplayName("Converte token inválido em resposta 401")
    void returnsUnauthorizedForInvalidToken() throws ServletException, IOException {
        SecurityFilter filter = new SecurityFilter(jwtService, userRepository);
        MockHttpServletRequest request = request("Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtService.validateToken("bad-token")).thenThrow(new ForbiddenException("Token inválido"));

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
        assertEquals("UTF-8", response.getCharacterEncoding());
        org.junit.jupiter.api.Assertions.assertTrue(response.getContentAsString().contains("Token inválido"));
    }

    private MockHttpServletRequest request(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/v1/rides");
        request.addHeader("Authorization", authorization);
        return request;
    }

    private User user(boolean verified) {
        User user = new User();
        user.setId("user-1");
        user.setEmail("user@example.com");
        user.setRole(Role.PASSENGER);
        user.setEmailVerified(verified);
        return user;
    }
}
