package com.ifride.core.service.driver.application;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.driver.model.dto.DriverApplicationRejectionDTO;
import com.ifride.core.driver.model.dto.DriverApplicationRequestDTO;
import com.ifride.core.driver.model.entity.DriverApplication;
import com.ifride.core.driver.model.enums.CnhCategory;
import com.ifride.core.driver.model.enums.DriverApplicationStatus;
import com.ifride.core.driver.model.mapper.DriverMapper;
import com.ifride.core.driver.repository.DriverApplicationRepository;
import com.ifride.core.driver.service.DriverApplicationService;
import com.ifride.core.driver.service.validators.DriverApplicationValidator;
import com.ifride.core.events.driver.model.DriverApplicationApprovedEvent;
import com.ifride.core.shared.exceptions.api.ConflictException;
import com.ifride.core.shared.exceptions.api.ForbiddenException;
import com.ifride.core.shared.exceptions.api.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverApplicationServiceTest {

    @InjectMocks
    private DriverApplicationService service;

    @Mock
    private DriverApplicationRepository repository;

    @Mock
    private UserService userService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private DriverMapper mapper;

    @Mock
    private DriverApplicationValidator validator;

    @Test
    @DisplayName("Create: Deve delegar validação e salvar com sucesso")
    void createDriverApplication_Success() {
        User author = new User();
        User user = new User();
        user.setId("user-123");
        user.setCpfEncrypted("36900423051");
        DriverApplicationRequestDTO dto = new DriverApplicationRequestDTO("123456", "666", CnhCategory.A, "cnh-security-number", LocalDate.now().plusYears(1));

        when(mapper.fromRequest(eq(dto), any(User.class))).thenReturn(new DriverApplication());
        when(userService.findById(user.getId())).thenReturn(user);
        when(repository.findAllByRequesterOrderByCreatedAtDesc(user)).thenReturn(List.of());
        when(repository.save(any(DriverApplication.class))).thenAnswer(i -> i.getArguments()[0]);

        service.createDriverApplication(author, user, dto);

        verify(validator).validateDriverApplication(eq(author), eq(user), any(), any());
        verify(repository).save(any(DriverApplication.class));
    }

    @Test
    @DisplayName("Create: Deve propagar ForbiddenException do validador")
    void createDriverApplication_ThrowsForbidden_WhenValidatorFails() {
        User author = new User();
        User user = new User();
        user.setCpfEncrypted("36900423051");
        DriverApplicationRequestDTO dto = new DriverApplicationRequestDTO("123", "B", CnhCategory.A, "cnh-security-number",LocalDate.now());

        doThrow(new ForbiddenException("Somente o próprio usuário"))
                .when(validator).validateDriverApplication(any(), any(), any(), any());

        assertThrows(ForbiddenException.class, () ->
                service.createDriverApplication(author, user, dto)
        );

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Approve: Deve aprovar e disparar evento após validar status")
    void approveDriverApplication_Success() {
        String userId = "user-id-123";
        User author = new User();
        author.setCpfEncrypted("36900423051");
        User requester = new User();
        requester.setCpfEncrypted("36900423051");
        DriverApplication pendingApp = new DriverApplication();
        pendingApp.setApplicationStatus(DriverApplicationStatus.PENDING);
        pendingApp.setRequester(requester);

        when(userService.findById(userId)).thenReturn(requester);
        when(repository.findAllByRequesterOrderByCreatedAtDesc(requester)).thenReturn(List.of(pendingApp));
        when(repository.save(any(DriverApplication.class))).thenAnswer(i -> i.getArguments()[0]);

        service.approveDriverApplication(author, userId);

        verify(validator).validateDriverApplicationStatusChange(pendingApp, DriverApplicationStatus.APPROVED);
        verify(eventPublisher).publishEvent(any(DriverApplicationApprovedEvent.class));
        verify(repository).save(pendingApp);
    }

    @Test
    @DisplayName("Approve: Deve falhar se o validador recusar a mudança de status")
    void approveDriverApplication_ThrowsConflict_WhenValidatorDenies() {
        String userId = "user-id-123";
        User requester = new User();
        DriverApplication app = new DriverApplication();

        when(userService.findById(userId)).thenReturn(requester);
        when(repository.findAllByRequesterOrderByCreatedAtDesc(requester)).thenReturn(List.of(app));

        doThrow(new ConflictException("Não é possível modificar..."))
                .when(validator).validateDriverApplicationStatusChange(any(), any());

        assertThrows(ConflictException.class, () ->
                service.approveDriverApplication(new User(), userId)
        );

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Reject: Deve rejeitar, salvar motivo e apagar código de segurança")
    void rejectDriverApplication_Success() {
        String userId = "user-id-123";
        User author = new User();
        DriverApplication pendingApp = new DriverApplication();
        pendingApp.setApplicationStatus(DriverApplicationStatus.PENDING);
        pendingApp.setCnhSecurityNumberEncrypted("encrypted-security");

        when(userService.findById(userId)).thenReturn(new User());
        when(repository.findAllByRequesterOrderByCreatedAtDesc(any())).thenReturn(List.of(pendingApp));
        when(repository.save(any(DriverApplication.class))).thenAnswer(i -> i.getArguments()[0]);

        service.rejectDriverApplication(author, userId, new DriverApplicationRejectionDTO("Documento ilegível"));

        assertNull(pendingApp.getCnhSecurityNumberEncrypted());
        verify(repository).save(pendingApp);
    }

    @Test
    @DisplayName("Security Code: Deve lançar erro se o código já foi expurgado")
    void findByDriverApplication_ThrowsNotFound_WhenSecurityCodeIsNull() {
        String appId = "app-123";
        DriverApplication app = new DriverApplication();
        app.setCnhSecurityNumberEncrypted(null);

        when(repository.findById(appId)).thenReturn(Optional.of(app));

        assertThrows(NotFoundException.class, () ->
                service.findByDriverApplication(appId)
        );
    }
}