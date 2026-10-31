package com.ifride.core.auth.controller;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "Gerenciamento de dados do usuário autenticado")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Exclui a própria conta (Soft Delete)",
            description = """
                    Realiza a exclusão lógica (Soft Delete) da conta do usuário autenticado.
                    
                    **Regras de Negócio (RN) & Segurança:**
                    * **Integridade Referencial:** O registro não é apagado fisicamente do banco de dados. O status da entidade muda para `DELETED`.
                    * **Preservação de Dados:** Isso garante que o histórico de caronas, veículos e pagamentos (que referenciam este usuário) não quebrem (Foreign Key Constraints).
                    * **Bloqueio de Acesso:** Usuários com status `DELETED` são automaticamente ocultados pelas queries do sistema (via `@SQLRestriction`) e não conseguem mais fazer login.
                    * **Privacidade (LGPD):** Embora o histórico seja mantido, os dados sensíveis de login e o CPF ficam inacessíveis.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Conta excluída com sucesso. Nenhum conteúdo é retornado."),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido, expirado ou usuário não autenticado.")
    })
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyAccount(@AuthenticationPrincipal User author) {
        userService.deleteAccount(author);
    }
}