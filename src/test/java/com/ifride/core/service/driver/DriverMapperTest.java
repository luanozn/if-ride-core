package com.ifride.core.service.driver;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.model.mapper.UserMapper;
import com.ifride.core.driver.model.dto.DriverApplicationRequestDTO;
import com.ifride.core.driver.model.entity.DriverApplication;
import com.ifride.core.driver.model.enums.CnhCategory;
import com.ifride.core.driver.model.mapper.DriverMapper;
import com.ifride.core.shared.services.DeterministicEncryptorService;
import com.ifride.core.shared.services.RandomizedEncryptorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DriverMapperTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private DeterministicEncryptorService deterministicCrypto;
    @Mock
    private RandomizedEncryptorService randomizedCrypto;

    @InjectMocks
    private DriverMapper mapper;

    private DriverApplication application;
    private User requester;

    @BeforeEach
    void setUp() {
        requester = new User();
        requester.setId("user-1");

        application = new DriverApplication();
        application.setRequester(requester);
        application.setCnhNumberEncrypted("encrypted-cnh");
        application.setCnhSecurityNumberEncrypted("encrypted-security");
        application.setCnhCategory(CnhCategory.B);
    }

    @Test
    @DisplayName("Deve criptografar dados corretos ao criar a partir do Request")
    void shouldEncryptDataFromRequest() {
        var dto = new DriverApplicationRequestDTO("user-1", "12345678901", CnhCategory.B, "1234", LocalDate.now());

        given(deterministicCrypto.encrypt("12345678901")).willReturn("encrypted-cnh");
        given(randomizedCrypto.encrypt("1234")).willReturn("encrypted-security");

        var result = mapper.fromRequest(dto, requester);

        assertThat(result.getCnhNumberEncrypted()).isEqualTo("encrypted-cnh");
        assertThat(result.getCnhSecurityNumberEncrypted()).isEqualTo("encrypted-security");
    }

    @Test
    @DisplayName("Deve revelar CNH para ADMIN")
    void shouldRevealCnhForAdmin() {
        var admin = new User();
        admin.setRole(Role.ADMIN);

        given(deterministicCrypto.decrypt("encrypted-cnh")).willReturn("12345678901");

        var dto = mapper.toDto(application, admin);

        assertThat(dto.cnhNumber()).isEqualTo("12345678901");
    }

    @Test
    @DisplayName("Deve ocultar CNH para outros usuários")
    void shouldHideCnhForOtherUsers() {
        var otherUser = new User();
        otherUser.setId("user-2");
        otherUser.setRole(Role.PASSENGER);

        var dto = mapper.toDto(application, otherUser);

        assertThat(dto.cnhNumber()).isNull();
    }

    @Test
    @DisplayName("Deve descriptografar o Security Code para o DTO")
    void shouldDecryptSecurityCode() {
        given(randomizedCrypto.decrypt("encrypted-security")).willReturn("1234");

        var result = mapper.fromDriverApplication(application);

        assertThat(result.securityNumber()).isEqualTo("1234");
    }
}