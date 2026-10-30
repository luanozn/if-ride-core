package com.ifride.core.service.events;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.service.EmailVerificationTokenService;
import com.ifride.core.events.user.listener.UserRegistrationListener;
import com.ifride.core.events.user.model.UserRegisteredEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserRegistrationListenerTest {

    @Mock
    private EmailVerificationTokenService tokenService;

    @InjectMocks
    private UserRegistrationListener listener;

    @Nested
    @DisplayName("Processamento de Registro de Usuário")
    class UserRegistrationProcessing {

        @Test
        @DisplayName("Deve gerar o token e enviar o e-mail de verificação ao receber o evento de registro")
        void shouldGenerateTokenAndSendEmail() {
            var user = new User();
            user.setId("user-1");
            user.setEmail("newuser@ifride.com");

            var event = new UserRegisteredEvent(user);

            listener.handleUserRegisteredEvent(event);
            verify(tokenService).generateTokenAndSendEmail(user);
        }
    }
}