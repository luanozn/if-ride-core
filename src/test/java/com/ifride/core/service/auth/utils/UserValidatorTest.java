package com.ifride.core.service.auth.utils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.auth.repository.UserRepository;
import com.ifride.core.auth.utils.UserValidator;
import com.ifride.core.shared.exceptions.api.BadRequestException;
import com.ifride.core.shared.exceptions.api.ConflictException;
import com.ifride.core.shared.services.DeterministicEncryptorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserValidatorTest {

    @Mock
    UserRepository repository;
    @Mock
    DeterministicEncryptorService encryptor;

    @InjectMocks
    UserValidator validator;

    private static final String VALID_EMAIL = "email@email.com";
    private static final String VALID_CPF_FORMATTED = "053.440.630-08";

    private RegisterRequestDTO requestWith(String email, String cpf) {
        return new RegisterRequestDTO(email, "senha123", "Nome Teste", cpf);
    }

    @Nested
    @DisplayName("Validação de formato de CPF")
    class CpfFormatValidation {

        @ParameterizedTest(name = "[{index}] CPF inválido: \"{0}\" - {1}")
        @CsvSource({
                "'123456789', 'Apenas 9 dígitos'",
                "'123.456.789-11', 'Dígito verificador inválido'",
                "'000.000.000-00', 'Todos os dígitos iguais'",
                "'111.111.111-11', 'Todos os dígitos iguais'",
                "'053.440.630-09', 'Dígito verificador errado por 1'",
                "'abc.def.ghi-jk', 'contém caracteres não numéricos'",
                "'123', 'valor digitado muito curto'",
                "'12345678901234567890', 'valor digitado muito longo'"
        })
        @DisplayName("Deve lançar BadRequestException para CPFs inválidos")
        void shouldThrowBadRequestForInvalidCpf(String invalidCpf, String reason) {
            var request = requestWith(VALID_EMAIL, invalidCpf);

            assertThatCode(() -> validator.validateUser(request))
                    .as("CPF inválido %s deveria ser rejeitado por %s", invalidCpf, reason)
                    .isInstanceOf(BadRequestException.class);
        }

        @ParameterizedTest(name = "[{index}] CPF válido: \"{0}\"")
        @ValueSource(strings = {
                "053.440.630-08",      // formatado
                "05344063008",         // sem formatação (mesmo CPF)
                "111.444.777-35",      // outro válido formatado
                "11144477735"          // outro válido sem formatação
        })
        @DisplayName("Não deve lançar exceção de formato para CPFs válidos")
        void shouldNotThrowForValidCpf(String validCpf) {
            given(repository.existsUserByEmail(anyString())).willReturn(false);
            given(repository.existsUserByCpfEncrypted(anyString())).willReturn(false);
            given(encryptor.encrypt(anyString())).willReturn("encrypted-cpf");

            var request = requestWith(VALID_EMAIL, validCpf);

            assertThatCode(() -> validator.validateUser(request))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Validação de unicidade")
    class UniquenessValidation {

        @ParameterizedTest(name = "[{index}] Email duplicado: \"{0}\"")
        @ValueSource(strings = {
                "email@email.com",
                "outro@dominio.com.br",
                "teste+tag@ifgoiano.edu.br"
        })
        @DisplayName("Deve lançar ConflictException quando email já existe")
        void shouldThrowConflictWhenEmailAlreadyExists(String duplicatedEmail) {
            given(repository.existsUserByEmail(duplicatedEmail)).willReturn(true);

            var request = requestWith(duplicatedEmail, VALID_CPF_FORMATTED);

            assertThatCode(() -> validator.validateUser(request))
                    .as("Email duplicado deveria ser rejeitado: %s", duplicatedEmail)
                    .isInstanceOf(ConflictException.class);
        }

        @ParameterizedTest(name = "[{index}] CPF duplicado: \"{0}\"")
        @ValueSource(strings = {
                "053.440.630-08",
                "111.444.777-35"
        })
        @DisplayName("Deve lançar ConflictException quando CPF já existe")
        void shouldThrowConflictWhenCpfAlreadyExists(String duplicatedCpf) {
            given(repository.existsUserByEmail(anyString())).willReturn(false);
            given(repository.existsUserByCpfEncrypted(anyString())).willReturn(true);
            given(encryptor.encrypt(anyString())).willReturn("encrypted-cpf");

            var request = requestWith(VALID_EMAIL, duplicatedCpf);

            assertThatCode(() -> validator.validateUser(request))
                    .as("CPF duplicado deveria ser rejeitado: %s", duplicatedCpf)
                    .isInstanceOf(ConflictException.class);
        }
    }

    @Test
    @DisplayName("Não deve lançar exceção quando todos os dados forem válidos e únicos")
    void shouldNotThrowWhenAllDataIsValidAndUnique() {
        given(repository.existsUserByEmail(anyString())).willReturn(false);
        given(repository.existsUserByCpfEncrypted(anyString())).willReturn(false);
        given(encryptor.encrypt(anyString())).willReturn("encrypted-cpf");

        var request = requestWith(VALID_EMAIL, VALID_CPF_FORMATTED);

        assertThatCode(() -> validator.validateUser(request)).doesNotThrowAnyException();
    }
}