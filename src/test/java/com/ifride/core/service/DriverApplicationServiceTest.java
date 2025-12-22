package com.ifride.core.service;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.driver.model.dto.DriverApplicationDTO;
import com.ifride.core.driver.model.dto.DriverApplicationRejectionDTO;
import com.ifride.core.driver.model.entity.Driver;
import com.ifride.core.driver.model.entity.DriverApplication;
import com.ifride.core.driver.model.enums.CnhCategory;
import com.ifride.core.driver.model.enums.DriverApplicationStatus;
import com.ifride.core.driver.repository.DriverApplicationRepository;
import com.ifride.core.driver.service.DriverApplicationService;
import com.ifride.core.driver.service.DriverService;
import com.ifride.core.shared.exceptions.api.ConflictException;
import com.ifride.core.shared.exceptions.api.ForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

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
    private DriverService driverService;

    @Mock
    private UserService userService;


    @Test
    @DisplayName("Create: Deve criar a aplicação com sucesso quando o autor for o usuário")
    void createDriverApplication_Success() {
        User author = mock(User.class);
        User user = mock(User.class);
        DriverApplicationDTO dto = new DriverApplicationDTO("123456", "B", CnhCategory.A , LocalDateTime.now().plusYears(1));

        when(author.getEmail()).thenReturn("user@email.com");
        when(user.getEmail()).thenReturn("user@email.com");
        when(user.has(Role.DRIVER)).thenReturn(false);
        when(repository.save(any(DriverApplication.class))).thenAnswer(i -> i.getArguments()[0]);

        DriverApplication result = service.createDriverApplication(author, user, dto);

        assertNotNull(result);
        assertEquals(DriverApplicationStatus.PENDING, result.getStatus());
        assertEquals(user, result.getRequester());
        assertEquals(dto.cnhNumber(), result.getCnhNumber());
        verify(repository).save(any(DriverApplication.class));
    }

    @Test
    @DisplayName("Create: Deve lançar ForbiddenExcepton quando o usuário requisitar pra outro usuário")
    void createDriverApplication_ThrowsForbidden_WhenUserIsSelf() {
        User user = mock(User.class);
        User author = mock(User.class);
        DriverApplicationDTO dto = new DriverApplicationDTO("123", "B", CnhCategory.A , LocalDateTime.now());

        when(user.getEmail()).thenReturn("user@email.com");
        when(author.getEmail()).thenReturn("admini@email.com");

        ForbiddenException exception = assertThrows(ForbiddenException.class, () ->
                service.createDriverApplication(author, user, dto)
        );

        assertEquals("Somente o próprio usuário pode solicitar para virar motorista!", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Create: Deve lançar ConflictException quando o usuário já for um motorista")
    void createDriverApplication_ThrowsConflict_WhenAlreadyDriver() {
        User user = mock(User.class);
        DriverApplicationDTO dto = new DriverApplicationDTO("123", "B", CnhCategory.A , LocalDateTime.now());

        when(user.getEmail()).thenReturn("user@email.com");
        when(user.has(Role.DRIVER)).thenReturn(true);

        assertThrows(ConflictException.class, () ->
                service.createDriverApplication(user, user, dto)
        );

        verify(repository, never()).save(any());
    }


    @Test
    @DisplayName("Approve: Deve provar uma solicitação com PENDING com sucesso")
    void approveDriverApplication_Success() {
        String userId = "user-id-123";
        User author = new User();
        User requester = new User();
        DriverApplication pendingApp = new DriverApplication();
        pendingApp.setStatus(DriverApplicationStatus.PENDING);
        pendingApp.setRequester(requester);

        when(userService.findById(userId)).thenReturn(requester);
        when(repository.findAllByRequesterOrderByCreatedAtDesc(requester)).thenReturn(List.of(pendingApp));
        when(repository.save(any(DriverApplication.class))).thenAnswer(i -> i.getArguments()[0]);
        when(driverService.saveFromDriverRequest(any(DriverApplication.class))).thenReturn(new Driver());

        Driver result = service.approveDriverApplication(author, userId);

        assertNotNull(result);
        assertEquals(DriverApplicationStatus.APPROVED, pendingApp.getStatus());
        assertEquals(author, pendingApp.getReviewedBy());
        assertNull(pendingApp.getRejectionReason());

        verify(driverService).saveFromDriverRequest(pendingApp);
        verify(repository).save(pendingApp);
    }

    @Test
    @DisplayName("Approve: Deve lançar uma ConflictException se o status não for PENDNG")
    void approveDriverApplication_ThrowsConflict_WhenNotPending() {
        String userId = "user-id-123";
        User author = new User();
        User requester = new User();
        DriverApplication rejectedApp = new DriverApplication();
        rejectedApp.setStatus(DriverApplicationStatus.DENIED);

        when(userService.findById(userId)).thenReturn(requester);
        when(repository.findAllByRequesterOrderByCreatedAtDesc(requester)).thenReturn(List.of(rejectedApp));

        ConflictException ex = assertThrows(ConflictException.class, () ->
                service.approveDriverApplication(author, userId)
        );

        assertTrue(ex.getMessage().contains("Não é possível modificar uma requisição que está com o status DENIED"));
        verify(repository, never()).save(any());
        verifyNoInteractions(driverService);
    }


    @Test
    @DisplayName("Reject: Deve rejetar com sucesso uma requsição com motivo.")
    void rejectDriverApplication_Success() {
        String userId = "user-id-123";
        User author = new User();
        User requester = new User();
        DriverApplicationRejectionDTO dto = new DriverApplicationRejectionDTO("CNH Inválida");

        DriverApplication pendingApp = new DriverApplication();
        pendingApp.setStatus(DriverApplicationStatus.PENDING);

        when(userService.findById(userId)).thenReturn(requester);
        when(repository.findAllByRequesterOrderByCreatedAtDesc(requester)).thenReturn(List.of(pendingApp));
        when(repository.save(any(DriverApplication.class))).thenAnswer(i -> i.getArguments()[0]);

        DriverApplication result = service.rejectDriverApplication(author, userId, dto);

        assertEquals(DriverApplicationStatus.DENIED, result.getStatus());
        assertEquals("CNH Inválida", result.getRejectionReason());
        assertEquals(author, result.getReviewedBy());

        verify(repository).save(pendingApp);
    }
}