package com.ifride.core.service;

import com.ifride.core.exceptions.ConflictException;
import com.ifride.core.model.auth.RegisterRequestDTO;
import com.ifride.core.model.auth.User;
import com.ifride.core.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService underTest;

    @Test
    public void shouldThrowConflictExceptionIfUserAlreadyExists() {
        var userMock = mock(User.class);
        var registerRequestDTO = mock(RegisterRequestDTO.class);

        given(userRepository.findByEmail(any(String.class))).willReturn(userMock);
        given(registerRequestDTO.email()).willReturn("email@email.com");

        assertThatThrownBy(() -> underTest.register(registerRequestDTO))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any(User.class));
    }
}
