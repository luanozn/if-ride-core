package com.ifride.core.admin.service;

import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.auth.model.dto.UserDto;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.model.mapper.UserMapper;
import com.ifride.core.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserService userService;
    private final UserMapper mapper;

    public User registerAdmin(RegisterRequestDTO registerRequest) {
        return this.userService.save(mapper.userFromRegisterRequest(registerRequest, Role.ADMIN));
    }

    public Page<UserDto> findAll(String search, Pageable pageable, User author) {
        return this.userService.findAll(Role.ADMIN, search, pageable, author);
    }

    public void delete(String administratorId) {
        User user = userService.findById(administratorId);
        userService.deleteAccount(user);
    }
}
