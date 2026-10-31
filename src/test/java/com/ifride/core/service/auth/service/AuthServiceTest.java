package com.ifride.core.service.auth.service;

import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.model.mapper.UserMapper;
import com.ifride.core.auth.repository.UserRepository;
import com.ifride.core.auth.service.AuthService;
import com.ifride.core.auth.utils.UserValidator;
import com.ifride.core.events.user.model.UserRegisteredEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository repository;
    @Mock
    UserMapper userMapper;
    @Mock
    UserValidator userValidator;
    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    AuthService authService;

    @Test
    @DisplayName("Deve registrar usuário, salvar no banco e publicar evento de registro")
    void shouldRegisterAndPublishEvent() {
        var request = new RegisterRequestDTO("teste@email.com", "senha123", "Teste", "05344063008", LocalDate.now());
        var mappedUser = new User();
        mappedUser.setEmail("teste@email.com");

        given(userMapper.userFromRegisterRequest(request, Role.PASSENGER)).willReturn(mappedUser);
        given(repository.save(mappedUser)).willReturn(mappedUser);

        var result = authService.register(request);

        assertThat(result).isEqualTo(mappedUser);

        verify(userValidator).validateUser(request);

        var eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().user()).isEqualTo(mappedUser);
    }
}