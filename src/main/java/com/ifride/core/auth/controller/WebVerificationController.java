package com.ifride.core.auth.controller;

import com.ifride.core.auth.model.EmailVerificationMaxAttemptsExceededException;
import com.ifride.core.auth.service.EmailVerificationTokenService;
import com.ifride.core.auth.service.UserService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Log4j2
@Controller
@RequestMapping("/v1/auth")
@AllArgsConstructor
@Tag(name = "Verificação Web", description = "Endpoints que retornam HTML, acionados por links de e-mail")
public class WebVerificationController {

    private final EmailVerificationTokenService emailVerificationTokenService;
    private final UserService userService;

    @Operation(
            summary = "Reenvia o e-mail de verificação (via formulário HTML)",
            description = """
                    Recebe um formulário submetido de uma página web estática para solicitar um novo e-mail de verificação.
                    
                    **Regras de Negócio (RN):**
                    * **Rate Limit:** Máximo de 3 tentativas a cada 15 minutos por usuário. Se exceder, a exceção `EmailVerificationMaxAttemptsExceededException` é capturada, mas o endpoint ainda retorna o HTML para evitar *User Enumeration* (descoberta de e-mails cadastrados).
                    * **Validade do Token:** Reutiliza o token se ainda estiver válido, ou gera um novo com TTL de 24 horas.
                    
                    **Retorno:** Um fragmento de HTML (`text/html`) com a mensagem de sucesso.
                    """,
            security = @SecurityRequirement(name = "")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Retorna o HTML de confirmação de reenvio, independentemente de o e-mail existir ou não."),
            @ApiResponse(responseCode = "400", description = "Requisição mal formatada (faltando o parâmetro email).")
    })
    @PostMapping(
            value = "/resend-verification-form",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public ResponseEntity<String> resendVerificationForm(@RequestParam String email) throws IOException {
        try {
            var user = userService.findByEmail(email);
            emailVerificationTokenService.generateTokenAndSendEmail(user);
        } catch (EmailVerificationMaxAttemptsExceededException e) {
            log.warn(e.getMessage());
        } catch (Exception e) {
            log.debug("Falha em reenvio via form para {}: {}", email, e.getMessage());
        }

        var html = new ClassPathResource("auth/verification-resend.html")
                .getContentAsString(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    @Operation(
            summary = "Valida o token de e-mail",
            description = """
                    Endpoint acionado quando o usuário clica no botão "Verificar" no e-mail da AWS SES.
                    
                    **Regras de Negócio (RN):**
                    * O token deve existir no banco, ser do tipo `EMAIL_VERIFICATION` e não estar expirado.
                    * Se válido, o usuário tem seu campo `emailVerified` alterado para `true` e o token é deletado (Single Use Token).
                    
                    **Retorno:** Não retorna JSON. Executa um **Redirect HTTP (302)**:
                    * Sucesso: Redireciona para `/auth/verification-success.html`.
                    * Falha: Redireciona para `/auth/verification-error.html` com a mensagem de erro na URL.
                    """,
            security = @SecurityRequirement(name = "")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirecionamento para a página HTML de sucesso ou erro."),
            @ApiResponse(responseCode = "400", description = "Token não fornecido na URL.")
    })
    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam("token") String token, RedirectAttributes redirectAttributes) {
        try {
            emailVerificationTokenService.confirmEmailVerification(token);
            return "redirect:/auth/verification-success.html";
        } catch (RuntimeException e) {
            redirectAttributes.addAttribute("errorMessage", e.getMessage());

            return "redirect:/auth/verification-error.html";
        }
    }
}