package com.ifride.core.auth.controller;

import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/users")
@AllArgsConstructor
@Log4j2
public class UserController {

    private final UserService userService;

    @PostMapping("/register/passenger")
    public User registerPassenger(@RequestBody RegisterRequestDTO registerDTO) {
        log.info("Started the PASSENGER registration for {}", registerDTO.email());
        return userService.register(registerDTO);
    }

    @PostMapping("/register/driver")
    @PreAuthorize("hasRole('PASSENGER')")
    public User registerDriver(
            @RequestBody RegisterRequestDTO registerDTO,
            @AuthenticationPrincipal User author
    ) {
        log.info("User {} started the DRIVER registration for {}", author.getEmail(), registerDTO.email());

        return userService.registerDriver(registerDTO, author);
    }

    @PostMapping("/register/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public User registerAdmin(
            @RequestBody RegisterRequestDTO registerDTO,
            @AuthenticationPrincipal User author
    ) {
        log.info("User {} started the ADMIN registration for {}", author.getEmail(), registerDTO.email());

        return userService.registerAdmin(registerDTO);
    }
}
