package com.ifride.core.admin.controller;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.admin.service.AdminUserService;
import com.ifride.core.auth.model.dto.RegisterRequestDTO;
import com.ifride.core.auth.model.dto.RegisterResponseDTO;
import com.ifride.core.auth.model.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/admins")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Log4j2
@Tag(name = "Gerência de Administradores", description = "Endpoints restritos a usuários com perfil ADMIN.")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminUserService adminService;

    @Operation(
            summary = "Cadastra um novo Administrador",
            description = """
                    Cria um novo usuário no sistema com a Role `ADMIN` de forma direta.
                    
                    **Segurança e Regras de Negócio:**
                    * **Acesso:** Apenas um Administrador já logado pode criar outro.
                    * **Validação de Dados:** O CPF passa pelo validador oficial (Caelum Stella) e pela checagem de unicidade.
                    * **Sem E-mail de Verificação:** Diferente do registro público, este endpoint não dispara o `UserRegisteredEvent`. O novo Admin já é criado como ativo e com e-mail validado.
                    * **Criptografia (LGPD):** O CPF é criptografado de forma determinística antes de ser persistido.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Administrador criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (Ex: CPF matematicamente inválido ou senha curta)."),
            @ApiResponse(responseCode = "401", description = "Não autenticado (Token inválido ou ausente)."),
            @ApiResponse(responseCode = "403", description = "Acesso negado. O usuário logado não possui a role ADMIN."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados: E-mail ou CPF já cadastrados no banco.")
    })
    @PostMapping("/users/admin")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponseDTO createAdmin(@Valid @RequestBody RegisterRequestDTO registerDTO) {
        log.info("Criando novo ADMIN: {}", registerDTO.email());
        return RegisterResponseDTO.from(adminService.registerAdmin(registerDTO));
    }

    @Operation(
            summary = "Lista todos os Administradores",
            description = """
                    Retorna uma página contendo todos os usuários com a Role `ADMIN`.
                    
                    **Regras de Negócio:**
                    * **Filtros:** Permite busca textual (`search`) por nome ou e-mail.
                    * **Visão de Dados (LGPD):** Como o requisitante é um ADMIN, o `UserMapper` retorna o DTO Completo (incluindo CPF descriptografado, e-mail e data de nascimento).
                    * **Paginação:** Utiliza `Pageable` do Spring, com ordenação padrão configurável via query params (`sort`, `size`, `page`).
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista paginada retornada com sucesso."),
            @ApiResponse(responseCode = "401", description = "Não autenticado."),
            @ApiResponse(responseCode = "403", description = "Acesso negado. Requer Role ADMIN.")
    })
    @GetMapping()
    public Page<UserDto> findAll(@AuthenticationPrincipal User author, @PageableDefault() Pageable pageable, @RequestParam(required = false) String search) {
        return adminService.findAll(search, pageable, author);
    }

    @Operation(
            summary = "Exclui um Administrador por ID",
            description = """
                    Realiza a exclusão lógica (Soft Delete) da conta do Administrador informado.
                    
                    **Regras de Negócio (RN) e Segurança:**
                    * **Soft Delete:** O registro não é apagado do banco (status = `DELETED`). Isso preserva a integridade referencial de históricos e logs de auditoria.
                    * **Bloqueio Imediato:** O usuário deletado perde o acesso instantaneamente (via `@SQLRestriction`).
                    * **Auto-Exclusão Bloqueada:** Um Administrador não pode usar este endpoint para deletar a si mesmo. Para isso, deve usar a rota genérica `DELETE /v1/users/me`.
                    * **Proteção da Invariante (Pessimistic Lock):** O sistema garante que sempre exista pelo menos um Administrador ativo. Para evitar condições de corrida (Write Skew) onde dois Admins se deletam simultaneamente, a transação utiliza `Pessimistic Locking` (`SELECT FOR UPDATE`). Se houver apenas 1 admin ativo, a operação é abortada com erro 403.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Administrador excluído com sucesso."),
            @ApiResponse(responseCode = "401", description = "Não autenticado."),
            @ApiResponse(responseCode = "403", description = "Acesso negado (Requer Role ADMIN, tentativa de auto-exclusão ou tentativa de excluir o último admin ativo)."),
            @ApiResponse(responseCode = "404", description = "Administrador não encontrado pelo ID informado.")
    })
    @DeleteMapping("/{administratorId}")
    public void delete(@AuthenticationPrincipal User author, @PathVariable String administratorId) {
        adminService.delete(administratorId, author);
    }
}