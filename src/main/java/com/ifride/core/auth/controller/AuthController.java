package com.ifride.core.auth.controller;

import com.ifride.core.auth.model.dto.LoginRequestDTO;
import com.ifride.core.auth.model.dto.LoginResponseDTO;
import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.auth.model.dto.RegisterResponseDTO;
import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.service.AuthService;
import com.ifride.core.auth.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Log4j2
@Tag(name = "Autenticação", description = "Endpoints para registro e login de usuários")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuthService authService;

    @Operation(
            summary = "Realiza o login do usuário",
            description = "Gera um token JWT válido para acessar endpoints protegidos.",
            security = @SecurityRequirement(name = "")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas ou usuário não encontrado")
    })
    @PostMapping("/login")
    public LoginResponseDTO login(@RequestBody LoginRequestDTO authBody) {
        var userPassword = new UsernamePasswordAuthenticationToken(authBody.email(), authBody.password());
        var auth = this.authenticationManager.authenticate(userPassword);

        return jwtService.generateLoginResponse((User) auth.getPrincipal());
    }


    @Operation(
            summary = "Registra um novo usuário no sistema",
            description = "Cria uma conta de PASSAGEIRO por padrão. O acesso de motorista deve ser solicitado posteriormente.",
            security = @SecurityRequirement(name = "")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado no sistema")
    })
    @PostMapping("/register")
    public RegisterResponseDTO register(@RequestBody RegisterRequestDTO registerDTO) {
        log.info("Novo registro público iniciado para {}", registerDTO.email());
        return RegisterResponseDTO.from(authService.register(registerDTO));
    }
}