package com.ifride.core.service.auth.service;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.repository.UserRepository;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.auth.utils.UserValidator;
import com.ifride.core.shared.exceptions.api.NotFoundException;
import com.ifride.core.shared.services.DeterministicEncryptorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String USER_ID = "288c8f5b-13ad-4f3b-b13d-4916a99b3141";

    @Mock
    UserRepository repository;

    @Mock
    UserValidator validator;

    @Mock
    DeterministicEncryptorService crypto;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    UserService userService;

    @Test
    @DisplayName("Deve buscar usuário por ID com sucesso")
    void shouldFindUserById() {
        var user = new User();
        user.setId(USER_ID);
        given(repository.findById(USER_ID)).willReturn(Optional.of(user));

        var result = userService.findById(USER_ID);

        assertThat(result).isEqualTo(user);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando ID não existir")
    void shouldThrowNotFoundWhenIdDoesNotExist() {
        given(repository.findById(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(USER_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Usuário " + USER_ID + " não encontrado");
    }

    @Nested
    @DisplayName("Processo de DELEÇÃO")
    class DeletionProcess {

        User user;

        @BeforeEach
        void setUp() {
            user = new User();
            user.setId(USER_ID);
            user.setName("Luan");
            user.setEmail("luan@ifride.com");
            user.setCpfEncrypted("encryptedCPF");
            user.setBirthDate(LocalDate.of(2005, 5, 3));
        }

        @Test
        @DisplayName("Deve anonimizar os dados e realizar o soft-delete da conta")
        void shouldDeleteAccountSuccessfully() {
            String expectedEncryptedId = "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3";
            given(crypto.encrypt(USER_ID)).willReturn(expectedEncryptedId);

            userService.deleteAccount(user);

            verify(crypto).encrypt(USER_ID);
            verify(repository).delete(user);

            assertThat(user)
                    .extracting(User::getId, User::getName, User::getEmail, User::getCpfEncrypted)
                    .containsExactly(USER_ID, "Usuário Deletado", "deleted_" + USER_ID + "@ifride.com", expectedEncryptedId);
        }
    }
}