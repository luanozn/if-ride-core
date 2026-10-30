package com.ifride.core.service.auth.service;

import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.model.mapper.UserMapper;
import com.ifride.core.shared.services.DeterministicEncryptorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class UserMapperTest {

    private UserMapper mapper;
    private DeterministicEncryptorService crypto;
    private PasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        crypto = mock(DeterministicEncryptorService.class);
        encoder = mock(PasswordEncoder.class);
        mapper = new UserMapper(crypto, encoder);
    }

    @Test
    @DisplayName("Deve mapear request de registro, criptografar CPF e hash da senha")
    void shouldMapFromRegisterRequest() {
        var request = new RegisterRequestDTO("teste@email.com", "senha123", "Teste", "05344063008", LocalDate.of(2000, 1, 1));

        given(encoder.encode("senha123")).willReturn("hashed_senha");
        given(crypto.encrypt(anyString())).willReturn("encrypted_cpf");

        var user = mapper.userFromRegisterRequest(request, Role.PASSENGER);

        assertThat(user.getEmail()).isEqualTo("teste@email.com");
        assertThat(user.getPassword()).isEqualTo("hashed_senha");
        assertThat(user.getCpfEncrypted()).isEqualTo("encrypted_cpf");
        assertThat(user.getRole()).isEqualTo(Role.PASSENGER);
    }

    @Test
    @DisplayName("Deve ocultar CPF e Data de Nascimento para usuários sem permissão")
    void shouldMaskDataForUnauthorizedUser() {
        var target = createUser("1", Role.PASSENGER);
        var requester = createUser("2", Role.PASSENGER);

        var dto = mapper.toDto(target, requester);

        assertThat(dto.cpf()).isNull(); // Mascarado
        assertThat(dto.birthDate()).isNull(); // Mascarado
        assertThat(dto.email()).isNull(); // Mascarado
        assertThat(dto.name()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Deve exibir dados completos para ADMIN")
    void shouldShowFullDataForAdmin() {
        var target = createUser("1", Role.PASSENGER);
        var admin = createUser("3", Role.ADMIN);

        given(crypto.decrypt("encrypted_cpf")).willReturn("05344063008");

        var dto = mapper.toDto(target, admin);

        assertThat(dto.cpf()).isEqualTo("053.440.630-08");
        assertThat(dto.birthDate()).isNotNull();
        assertThat(dto.email()).isEqualTo("john@email.com");
    }

    private User createUser(String id, Role role) {
        var u = new User();
        u.setId(id);
        u.setName("John Doe");
        u.setEmail("john@email.com");
        u.setRole(role);
        u.setBirthDate(LocalDate.of(1990, 1, 1));
        u.setCpfEncrypted("encrypted_cpf");
        return u;
    }
}