package com.ifride.core.controller;

import com.ifride.core.model.auth.LoginRequestDTO;
import com.ifride.core.model.auth.RegisterRequestDTO;
import com.ifride.core.model.auth.User;
import com.ifride.core.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("v1/auth")
@AllArgsConstructor
public class AuthorizationController {
    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody LoginRequestDTO authBody) {
        var userPassword = new UsernamePasswordAuthenticationToken(authBody.email(), authBody.password());
        var auth = this.authenticationManager.authenticate(userPassword);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/register")
    public User register(@RequestBody RegisterRequestDTO registerDTO) {
        return userService.register(registerDTO);
    }
}
