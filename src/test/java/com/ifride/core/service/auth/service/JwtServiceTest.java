package com.ifride.core.service.auth.service;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("um-secret-muito-seguro-para-teste");

        user = new User();
        user.setEmail("teste@ifride.com");
        user.setRole(Role.PASSENGER);
    }

    @Test
    @DisplayName("Deve gerar um token válido e conseguir extrair o subject (email)")
    void shouldGenerateAndValidateToken() {
        var token = jwtService.generateToken(user, jwtService.generateExpirationDate());

        assertThat(token).isNotBlank();

        var subject = jwtService.validateToken(token);
        assertThat(subject).isEqualTo("teste@ifride.com");
    }

    @Test
    @DisplayName("Deve gerar o DTO de login completo com as informações do usuário")
    void shouldGenerateLoginResponse() {
        var response = jwtService.generateLoginResponse(user);

        assertThat(response.token()).isNotBlank();
        assertThat(response.login()).isEqualTo("teste@ifride.com");
        assertThat(response.expireDate()).isNotNull();
    }
}