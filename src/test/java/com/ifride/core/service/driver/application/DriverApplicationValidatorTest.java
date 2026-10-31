package com.ifride.core.service.driver.application;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.driver.model.dto.DriverApplicationRequestDTO;
import com.ifride.core.driver.model.entity.DriverApplication;
import com.ifride.core.driver.model.enums.CnhCategory;
import com.ifride.core.driver.model.enums.DriverApplicationStatus;
import com.ifride.core.driver.repository.DriverApplicationRepository;
import com.ifride.core.driver.service.validators.DriverApplicationValidator;
import com.ifride.core.shared.exceptions.api.ConflictException;
import com.ifride.core.shared.exceptions.api.ForbiddenException;
import com.ifride.core.shared.services.DeterministicEncryptorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DriverApplicationValidatorTest {

    @Mock
    private DriverApplicationRepository repository;
    @Mock
    private DeterministicEncryptorService crypto;

    @InjectMocks
    private DriverApplicationValidator validator;

    private User author;
    private User targetUser;
    private DriverApplicationRequestDTO validDto;

    @BeforeEach
    void setUp() {
        author = new User();
        author.setId("user-1");
        author.setEmail("author@email.com");
        author.setRole(Role.PASSENGER);

        targetUser = new User();
        targetUser.setId("user-1");
        targetUser.setEmail("author@email.com");
        targetUser.setRole(Role.PASSENGER);

        validDto = new DriverApplicationRequestDTO(
                "user-1", "12345678901", CnhCategory.B, "1234", LocalDate.now().plusYears(1)
        );
    }

    @Test
    @DisplayName("Deve validar com sucesso quando tudo está correto")
    void shouldValidateSuccessfully() {
        given(crypto.encrypt(anyString())).willReturn("encrypted-cnh");
        given(repository.findByCnhNumberEncryptedAndApplicationStatusIn(anyString(), any())).willReturn(Optional.empty());

        assertThatCode(() -> validator.validateDriverApplication(author, targetUser, Optional.empty(), validDto))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException se o autor tentar aplicar para outro usuário")
    void shouldThrowForbiddenWhenApplyingForOthers() {
        User anotherUser = new User();
        anotherUser.setId("user-2");
        anotherUser.setEmail("outro@email.com");

        assertThatThrownBy(() -> validator.validateDriverApplication(author, anotherUser, Optional.empty(), validDto))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Somente o próprio usuário");
    }

    @Test
    @DisplayName("Deve lançar ConflictException se o usuário já for motorista")
    void shouldThrowConflictWhenUserIsAlreadyDriver() {
        targetUser.setRole(Role.DRIVER);

        assertThatThrownBy(() -> validator.validateDriverApplication(author, targetUser, Optional.empty(), validDto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("já é um motorista");
    }

    @Test
    @DisplayName("Deve lançar ConflictException se o usuário já tiver aplicação PENDENTE")
    void shouldThrowConflictWhenUserHasPendingApplication() {
        DriverApplication pendingApp = new DriverApplication();
        pendingApp.setApplicationStatus(DriverApplicationStatus.PENDING);
        pendingApp.setRequester(targetUser);

        given(repository.existsByRequesterAndApplicationStatus(targetUser, DriverApplicationStatus.APPROVED)).willReturn(false);

        assertThatThrownBy(() -> validator.validateDriverApplication(author, targetUser, Optional.of(pendingApp), validDto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ainda tem uma requisição pendente");
    }

    @Test
    @DisplayName("Deve lançar ConflictException se a CNH já estiver em uso")
    void shouldThrowConflictWhenCnhIsAlreadyInUse() {
        given(crypto.encrypt(anyString())).willReturn("encrypted-cnh");
        given(repository.findByCnhNumberEncryptedAndApplicationStatusIn(anyString(), any())).willReturn(Optional.of(new DriverApplication()));

        assertThatThrownBy(() -> validator.validateDriverApplication(author, targetUser, Optional.empty(), validDto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("já está cadastrada");
    }
}