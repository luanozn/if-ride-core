package com.ifride.core.admin.service;

import com.ifride.core.admin.DriverDirectlyDTO;
import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.model.enums.Role;
import com.ifride.core.auth.repository.UserRepository;
import com.ifride.core.auth.service.converter.UserConverter;
import com.ifride.core.driver.model.dto.DriverApplicationRequestDTO;
import com.ifride.core.driver.service.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository repository;
    private final UserConverter userConverter;
    private final DriverService driverService;

    public User registerAdmin(RegisterRequestDTO registerRequest) {
        return this.repository.save(userConverter.from(registerRequest, Role.ADMIN));
    }

    public User registerDriverDirectly(DriverDirectlyDTO driverDirectlyDTO) {
        driverService.saveFromDTO(driverDirectlyDTO.driverInformation());
        return this.repository.save(userConverter.from(driverDirectlyDTO.userInfo(), Role.DRIVER));
    }
}
