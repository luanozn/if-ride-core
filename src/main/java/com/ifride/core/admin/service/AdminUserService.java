package com.ifride.core.admin.service;

import com.ifride.core.admin.AdminValidator;
import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.auth.model.dto.UserDto;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.model.mapper.UserMapper;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.auth.utils.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserService userService;
    private final UserMapper mapper;
    private final AdminValidator adminValidator;
    private final UserValidator userValidator;

    public User registerAdmin(RegisterRequestDTO registerRequest) {
        userValidator.validateUser(registerRequest);
        User admin = mapper.userFromRegisterRequest(registerRequest, Role.ADMIN);
        admin.setEmailVerified(true);
        return this.userService.save(admin);
    }

    public Page<UserDto> findAll(String search, Pageable pageable, User author) {
        return this.userService.findAll(Role.ADMIN, search, pageable, author);
    }

    public void delete(String administratorId, User author) {
        adminValidator.validateNotSelfDeletion(administratorId, author);

        User user = userService.findById(administratorId);
        userService.deleteAccount(user);
    }
}
