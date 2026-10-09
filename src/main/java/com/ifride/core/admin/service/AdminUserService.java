package com.ifride.core.admin.service;

import com.ifride.core.admin.DriverDirectlyDTO;
import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.auth.model.dto.UserDto;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.auth.service.converter.UserConverter;
import com.ifride.core.driver.service.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserService userService;
    private final UserConverter userConverter;
    private final DriverService driverService;

    public User registerAdmin(RegisterRequestDTO registerRequest) {
        return this.userService.save(userConverter.from(registerRequest, Role.ADMIN));
    }

    public User registerDriverDirectly(DriverDirectlyDTO driverDirectlyDTO) {
        driverService.saveFromDTO(driverDirectlyDTO.driverInformation());
        return this.userService.save(userConverter.from(driverDirectlyDTO.userInfo(), Role.DRIVER));
    }

    public Page<UserDto> findAll(String search, Pageable pageable) {
        return this.userService.findAll(Role.ADMIN, search, pageable).map(UserDto::fromEntity);
    }

    public void delete(String administratorId) {
        User user = userService.findById(administratorId);
        userService.deleteAccount(user);
    }
}
