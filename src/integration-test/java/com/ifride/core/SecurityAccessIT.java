package com.ifride.core;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.shared.config.ErrorResponse;
import com.ifride.core.shared.test.BaseIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;


import static org.assertj.core.api.BDDAssertions.then;

class SecurityAccessIT extends BaseIT {

    private String passengerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        // Setup de usuários para os testes de Role
        User passenger = createUser(Role.PASSENGER);
        User admin = createUser(Role.ADMIN);

        passengerToken = getToken(passenger.getEmail());
        adminToken = getToken(admin.getEmail());
    }

    @Nested
    @DisplayName("Bloqueio de Acesso Direto (GatewayOriginFilter)")
    class GatewayOriginTests {

        @Test
        @DisplayName("Deve retornar 403 ao tentar acessar sem o header X-Origin")
        void shouldBlockRequestWithoutOriginHeader() {
            // Tenta acessar uma rota protegida (ou pública), mas SEM o header X-Origin
            var response = rest.getForEntity("/v1/users/me", ErrorResponse.class);

            then(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            then(response.getBody()).isNotNull();
            // O GatewayOriginFilter intercepta antes do Spring Security e devolve esse JSON
            then(response.getBody().getMessage()).isNull(); // O Filter devolve um JSON manual {"error":"..."}
        }
    }

    @Nested
    @DisplayName("Autenticação (Spring Security e JWT)")
    class AuthenticationTests {

        @Test
        @DisplayName("Deve retornar 403 (ou 401) ao tentar acessar rota protegida sem token")
        void shouldBlockRequestWithoutToken() {
            var response = rest.exchange(
                    "/v1/users/me",
                    HttpMethod.DELETE,
                    new HttpEntity<>(createHeaders(null)),
                    ErrorResponse.class
            );

            then(response.getStatusCode().is4xxClientError()).isTrue();
        }

        @Test
        @DisplayName("Deve retornar 403 ao tentar acessar rota protegida com token inválido")
        void shouldBlockRequestWithInvalidToken() {
            var response = rest.exchange(
                    "/v1/users/me",
                    HttpMethod.DELETE,
                    new HttpEntity<>(createHeaders("token.invalido.aqui")),
                    ErrorResponse.class
            );

            then(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("Autorização (Roles e PreAuthorize)")
    class AuthorizationTests {

        @Test
        @DisplayName("Deve retornar 403 quando um PASSENGER tentar acessar rota de ADMIN")
        void shouldBlockPassengerAccessingAdminRoute() {
            var response = rest.exchange(
                    "/v1/admins",
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders(passengerToken)),
                    ErrorResponse.class
            );

            then(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            then(response.getBody()).isNotNull();
            then(response.getBody().getMessage()).contains("Acesso negado");
            then(response.getBody().getStatusCode()).isEqualTo(403);
        }

        @Test
        @DisplayName("Deve permitir que um ADMIN acesse rota de PASSENGER (Prova da Herança de Role)")
        void shouldAllowAdminAccessingPassengerRoute() {
            var response = rest.exchange(
                    "/v1/driver-requests/me",
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders(adminToken)),
                    ErrorResponse.class
            );

            then(response.getStatusCode()).isNotEqualTo(HttpStatus.FORBIDDEN);
            then(response.getBody().getMessage()).contains("Nenhuma solicitação encontrada");
        }
    }
}