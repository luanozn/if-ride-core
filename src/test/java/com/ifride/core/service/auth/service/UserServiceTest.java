package com.ifride.core.service.auth.service;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.repository.UserRepository;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.shared.exceptions.api.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository repository;

    @InjectMocks
    UserService userService;

    @Test
    @DisplayName("Deve buscar usuário por ID com sucesso")
    void shouldFindUserById() {
        var user = new User();
        user.setId("123");
        given(repository.findById("123")).willReturn(Optional.of(user));

        var result = userService.findById("123");

        assertThat(result).isEqualTo(user);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando ID não existir")
    void shouldThrowNotFoundWhenIdDoesNotExist() {
        given(repository.findById("123")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById("123"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Usuário 123 não encontrado");
    }

    @Test
    @DisplayName("Deve realizar o soft-delete da conta")
    void shouldDeleteAccount() {
        var user = new User();

        userService.deleteAccount(user);

        verify(repository).delete(user);
    }
}