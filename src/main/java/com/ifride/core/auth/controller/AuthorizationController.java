package com.ifride.core.auth.controller;

import com.ifride.core.auth.model.dto.LoginRequestDTO;
import com.ifride.core.auth.model.dto.LoginResponseDTO;
import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.service.EmailVerificationTokenService;
import com.ifride.core.auth.service.JwtService;
import com.ifride.core.auth.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("v1/auth")
@AllArgsConstructor
public class AuthorizationController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final EmailVerificationTokenService emailVerificationTokenService;

    @PostMapping("/login")
    public LoginResponseDTO login(@RequestBody LoginRequestDTO authBody) {
        var userPassword = new UsernamePasswordAuthenticationToken(authBody.email(), authBody.password());
        var auth = this.authenticationManager.authenticate(userPassword);

        return jwtService.generateLoginResponse((User) auth.getPrincipal());
    }

    // TODO: Remove this endpoint (Used for testing only)
    @PostMapping("/registerAdmin")
    public void registerAdmin(@RequestBody RegisterRequestDTO registerRequestDTO) {
        userService.registerAdmin(registerRequestDTO);
    }

    @GetMapping("/verify-email")
    public void verifyEmail(@RequestParam String token) {
        emailVerificationTokenService.confirmEmailVerification(token);
    }

}
