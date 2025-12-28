package com.ifride.core.admin.controller;

import com.ifride.core.admin.service.AdminUserService;
import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.auth.model.dto.RegisterResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Log4j2
public class AdminController {

    private final AdminUserService adminService;

    @PostMapping("/users/admin")
    public RegisterResponseDTO createAdmin(@RequestBody RegisterRequestDTO registerDTO) {
        log.info("Criando novo ADMIN: {}", registerDTO.email());
        return RegisterResponseDTO.from(adminService.registerAdmin(registerDTO));
    }

    @PostMapping("/users/driver-direct")
    public RegisterResponseDTO createDriverDirectly(@RequestBody RegisterRequestDTO dto) {
        return RegisterResponseDTO.from(adminService.registerDriverDirectly(dto));
    }
}