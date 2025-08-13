package com.ifride.core.controller;

import com.ifride.core.model.auth.LoginRequestDTO;
import com.ifride.core.model.auth.LoginResponseDTO;
import com.ifride.core.model.auth.RegisterRequestDTO;
import com.ifride.core.model.auth.User;
import com.ifride.core.service.TokenService;
import com.ifride.core.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("v1/auth")
@AllArgsConstructor
public class AuthorizationController {
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UserService userService;

    @PostMapping("/login")
    public LoginResponseDTO login(@RequestBody LoginRequestDTO authBody) {
        var userPassword = new UsernamePasswordAuthenticationToken(authBody.email(), authBody.password());
        var auth = this.authenticationManager.authenticate(userPassword);

        return tokenService.generateLoginResponse((User) auth.getPrincipal());
    }

    // TODO: Remove this endpoint (Used for testing only)
    @PostMapping("/registerAdmin")
    public void registerAdmin(@RequestBody RegisterRequestDTO registerRequestDTO) {
        userService.registerAdmin(registerRequestDTO);
    }

}
