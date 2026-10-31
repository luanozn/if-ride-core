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
                    Este endpoint é de uso genérico e atende a todos os perfis (PASSENGER, DRIVER e ADMIN).
                    
                    **Regras de Negócio (RN) & Segurança:**
                    * **Integridade Referencial:** O registro não é apagado fisicamente. O status da entidade muda para `DELETED`, preservando o histórico de caronas, veículos e logs de auditoria.
                    * **Bloqueio de Acesso:** Usuários com status `DELETED` são ocultados pelas queries do sistema (via `@SQLRestriction`) e não conseguem mais fazer login.
                    * **Privacidade (LGPD):** O histórico é mantido, mas os dados de login e o CPF criptografado tornam-se inacessíveis.
                    * **Proteção da Invariante (Admins):** Se o usuário autenticado for um ADMINISTRADOR, o sistema aciona a trava de segurança de "último admin". O banco de dados executa um `Pessimistic Lock` (`SELECT FOR UPDATE`) para evitar condições de corrida (Write Skew). Se for o último admin ativo, a operação é abortada com HTTP 403.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Conta excluída com sucesso. Nenhum conteúdo é retornado."),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido, expirado ou usuário não autenticado."),
            @ApiResponse(responseCode = "403", description = "Acesso negado. O usuário é o único Administrador ativo do sistema e não pode ser excluído.")
    })
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyAccount(@AuthenticationPrincipal User author) {
        userService.deleteAccount(author);
    }
}