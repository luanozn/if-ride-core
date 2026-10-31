package com.ifride.core.service.admin;

import com.ifride.core.admin.AdminValidator;
import com.ifride.core.admin.service.AdminUserService;
import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.auth.model.dto.UserDto;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.model.mapper.UserMapper;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.auth.utils.UserValidator;
import com.ifride.core.shared.exceptions.api.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private UserMapper mapper;
    @Mock
    private AdminValidator adminValidator;
    @Mock
    private UserValidator userValidator;

    @InjectMocks
    private AdminUserService adminUserService;

    private User author;
    private RegisterRequestDTO validRequest;

    @BeforeEach
    void setUp() {
        author = new User();
        author.setId("admin-123");

        validRequest = new RegisterRequestDTO(
                "admin@ifride.com", "senha123", "Admin Teste", "05344063008", LocalDate.of(1990, 1, 1)
        );
    }

    @Nested
    @DisplayName("Cadastro de Administrador")
    class RegisterAdminTests {

        @Test
        @DisplayName("Deve validar, mapear e salvar um novo administrador com email verificado")
        void shouldRegisterAdminSuccessfully() {
            User mappedUser = new User();
            mappedUser.setRole(Role.ADMIN);

            given(mapper.userFromRegisterRequest(validRequest, Role.ADMIN)).willReturn(mappedUser);
            given(userService.save(mappedUser)).willReturn(mappedUser);

            var result = adminUserService.registerAdmin(validRequest);

            verify(userValidator).validateUser(validRequest);
            verify(mapper).userFromRegisterRequest(validRequest, Role.ADMIN);
            verify(userService).save(mappedUser);

            assertThat(mappedUser.isEmailVerified()).isTrue();
            assertThat(result).isEqualTo(mappedUser);
        }
    }

    @Nested
    @DisplayName("Listagem de Administradores")
    class FindAllTests {

        @Test
        @DisplayName("Deve delegar a busca para o UserService filtrando por Role ADMIN")
        void shouldFindAllAdmins() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<UserDto> expectedPage = new PageImpl<>(List.of(new UserDto(
                    "1", "Admin", "admin@email.com", "05344063008", Role.ADMIN, LocalDate.now(), true
            )));

            given(userService.findAll(Role.ADMIN, "search-term", pageable, author)).willReturn(expectedPage);

            var result = adminUserService.findAll("search-term", pageable, author);

            assertThat(result).isEqualTo(expectedPage);
            verify(userService).findAll(Role.ADMIN, "search-term", pageable, author);
        }
    }

    @Nested
    @DisplayName("Exclusão de Administrador")
    class DeleteTests {

        @Test
        @DisplayName("Deve deletar com sucesso quando os dados forem válidos")
        void shouldDeleteAdminSuccessfully() {
            String targetId = "admin-456";
            User targetUser = new User();
            targetUser.setId(targetId);

            given(userService.findById(targetId)).willReturn(targetUser);

            adminUserService.delete(targetId, author);

            verify(adminValidator).validateNotSelfDeletion(targetId, author);
            verify(userService).findById(targetId);
            verify(userService).deleteAccount(targetUser);
        }

        @Test
        @DisplayName("Não deve deletar se a validação de auto-exclusão falhar")
        void shouldNotDeleteWhenValidationFails() {
            String targetId = "admin-123";

            willThrow(new ForbiddenException("Você não pode deletar seu próprio usuário"))
                    .given(adminValidator).validateNotSelfDeletion(targetId, author);

            assertThatThrownBy(() -> adminUserService.delete(targetId, author))
                    .isInstanceOf(ForbiddenException.class);

            verify(userService, org.mockito.Mockito.never()).findById(any());
            verify(userService, org.mockito.Mockito.never()).deleteAccount(any());
        }
    }
}