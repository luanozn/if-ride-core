package com.ifride.core.service.auth.utils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.auth.repository.UserRepository;
import com.ifride.core.auth.utils.UserValidator;
import com.ifride.core.shared.exceptions.api.BadRequestException;
import com.ifride.core.shared.exceptions.api.ConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserValidatorTest {
    @Mock
    UserRepository repository;
    @InjectMocks
    UserValidator validator;

    @Test
    @DisplayName("Deve lançar BadRequest se o número de CPF não for válido para um cpf não formatado")
    void shouldThrowBadRequestWhenUnformattedInvalidCPF() {
        var mockedDTO = mock(RegisterRequestDTO.class);

        given(mockedDTO.documentNumber()).willReturn("123456789");
        assertThatCode(() -> validator.validateDocument(mockedDTO)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Deve lançar BadRequest se o número de CPF não for válido para um cpf não formatado")
    void shouldThrowBadRequestWhenFormattedInvalidCPF() {
        var mockedDTO = mock(RegisterRequestDTO.class);

        given(mockedDTO.documentNumber()).willReturn("123.456.789-11");
        assertThatCode(() -> validator.validateDocument(mockedDTO)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Não deve lançar exceção se o todas as informações forem válidas")
    void shouldThrowBadRequestWhenInvalidCPF() {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "email@email.com",
                "",
                "",
                "053.440.630-08"
        );

        given(repository.existsUserByEmail(anyString())).willReturn(false);
        given(repository.existsUserByCpf(anyString())).willReturn(false);

        assertThatCode(() -> validator.validateDocument(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve lançar exceção de conflito se o email já existir")
    void shouldThrowConflictWhenEmailAlreadyExists() {
        var mockedDTO = mock(RegisterRequestDTO.class);

        given(repository.existsUserByEmail("email@email.com")).willReturn(true);

        given(mockedDTO.documentNumber()).willReturn("053.440.630-08");
        given(mockedDTO.email()).willReturn("email@email.com");
        assertThatCode(() -> validator.validateDocument(mockedDTO)).isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Deve lançar exceção de conflito se o CPF já existir")
    void shouldThrowConflictWhenCpfAlreadyExists() {
        var mockedDTO = mock(RegisterRequestDTO.class);

        given(repository.existsUserByEmail("email@email.com")).willReturn(false);
        given(repository.existsUserByCpf("05344063008")).willReturn(true);

        given(mockedDTO.documentNumber()).willReturn("053.440.630-08");
        given(mockedDTO.email()).willReturn("email@email.com");
        assertThatCode(() -> validator.validateDocument(mockedDTO)).isInstanceOf(ConflictException.class);
    }
}
