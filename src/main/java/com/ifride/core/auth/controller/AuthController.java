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
@Tag(name = "Autenticação", description = "Endpoints para registro, login e gestão de credenciais")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuthService authService;

    @Operation(
            summary = "Realiza o login do usuário",
            description = """
                    Autentica o usuário e gera um token JWT para acesso às rotas protegidas.

                    **Regras de Negócio (RN):**
                    * **Credenciais:** O e-mail deve existir e a senha deve bater com o hash BCrypt salvo.
                    * **Status da Conta:** Usuários com status `DELETED` (Soft Delete) não conseguem logar.
                    * **Validade do Token:** O token gerado tem um tempo de expiração (TTL) padrão de 6 horas.
                    
                    **Nota:** A autenticação básica (Basic Auth) do Spring Security é utilizada internamente para validar as credenciais antes de gerar o JWT.
                    """,
            security = @SecurityRequirement(name = "")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso. Retorna o Token JWT e os dados do usuário."),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas, e-mail não encontrado ou conta deletada."),
            @ApiResponse(responseCode = "400", description = "Corpo da requisição mal formatado (e-mail ou senha ausentes).")
    })
    @PostMapping("/login")
    public LoginResponseDTO login(@RequestBody LoginRequestDTO authBody) {
        var userPassword = new UsernamePasswordAuthenticationToken(authBody.email(), authBody.password());
        var auth = this.authenticationManager.authenticate(userPassword);

        return jwtService.generateLoginResponse((User) auth.getPrincipal());
    }

    @Operation(
            summary = "Registra um novo passageiro",
            description = """
                    Cria uma nova conta de usuário com a role inicial `PASSENGER`.

                    **Regras de Negócio (RN) & Segurança:**
                    * **Validação de CPF:** O CPF é validado matematicamente (algoritmo de dígitos verificadores).
                    * **Unicidade:** O e-mail e o CPF não podem existir no sistema.
                    * **LGPD e Criptografia:** O CPF é criptografado de forma **Determinística** antes de ser salvo no banco (permitindo buscas por igualdade).
                    * **Senha:** A senha é transformada em hash usando BCrypt.
                    
                    **Efeitos Colaterais:**
                    * Dispara um evento assíncrono (`UserRegisteredEvent`) que gera um token de verificação e envia um e-mail (AWS SES) para o usuário.
                    """,
            security = @SecurityRequirement(name = "")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário registrado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos (Ex: CPF inválido ou senha com menos de 8 caracteres)."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados: E-mail ou CPF já cadastrado no sistema.")
    })
    @PostMapping("/register")
    public RegisterResponseDTO register(@RequestBody RegisterRequestDTO registerDTO) {
        log.info("Novo registro público iniciado para {}", registerDTO.email());
        return RegisterResponseDTO.from(authService.register(registerDTO));
    }
}