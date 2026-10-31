package com.ifride.core.driver.controller;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.service.UserService;
import com.ifride.core.driver.model.dto.CnhSecurityNumberDTO;
import com.ifride.core.driver.model.dto.DriverApplicationRejectionDTO;
import com.ifride.core.driver.model.dto.DriverApplicationRequestDTO;
import com.ifride.core.driver.model.dto.DriverApplicationSummaryDTO;
import com.ifride.core.driver.model.enums.DriverApplicationStatus;
import com.ifride.core.driver.service.DriverApplicationService;
import com.ifride.core.shared.audit.service.AuditLogService;
import com.ifride.core.shared.utils.HttpRequestUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/driver-requests")
@RequiredArgsConstructor
@Log4j2
@Tag(
        name = "Solicitações de Upgrade à Motorista",
        description = "Fluxo de upgrade de conta PASSENGER → DRIVER, com análise de documentos e auditoria."
)
@SecurityRequirement(name = "bearerAuth")
public class DriverApplicationController {

    private final UserService userService;
    private final DriverApplicationService driverApplicationService;
    private final AuditLogService auditLogService;

    @Operation(
            summary = "Retorna a última solicitação do passageiro logado",
            description = """
                    Consulta a solicitação mais recente do próprio passageiro autenticado.
                    
                    **Regras de Negócio (RN):**
                    * **Escopo:** O passageiro só consegue ver a sua própria solicitação, nunca a de terceiros.
                    * **Visão de Dados (LGPD):** Por ser o próprio dono, o retorno é o DTO completo (CPF e data de nascimento inclusos).
                    * **Retorno:** A solicitação mais recente é retornada, independentemente do status (PENDING, APPROVED ou DENIED).
                    
                    **Diferença para o endpoint ADMIN (`GET /{applicationId}`):** Este endpoint (`/me`) usa o ID do usuário autenticado como filtro; o outro usa o ID da aplicação.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitação encontrada."),
            @ApiResponse(responseCode = "401", description = "Não autenticado."),
            @ApiResponse(responseCode = "403", description = "Acesso negado. Requer Role PASSENGER."),
            @ApiResponse(responseCode = "404", description = "Nenhuma solicitação encontrada para este usuário.")
    })
    @GetMapping("/me")
    @PreAuthorize("hasRole('PASSENGER')")
    public DriverApplicationSummaryDTO getMyApplication(@AuthenticationPrincipal User author) {
        return driverApplicationService.getMyApplication(author);
    }

    @Operation(
            summary = "Inicia um pedido para tornar-se motorista",
            description = """
                    Cria uma nova solicitação de upgrade de conta para MOTORISTA.
                    
                    **Regras de Negócio (RN):**
                    * **Auto-solicitação:** O `requesterId` do body deve ser igual ao ID do usuário autenticado.
                    * **Unicidade:** Um usuário não pode ter duas solicitações PENDING ou APPROVED simultâneas.
                    * **CNH Única:** A CNH informada não pode estar em uso em outra aplicação PENDING ou APPROVED.
                    * **Recorrência:** Se o usuário já foi DENIED antes, ele pode tentar novamente (não há lock permanente).
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Solicitação criada com status PENDING."),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (Ex: CNH vazia ou categoria inválida)."),
            @ApiResponse(responseCode = "401", description = "Não autenticado."),
            @ApiResponse(responseCode = "403", description = "Tentativa de solicitar em nome de outro usuário, ou usuário já é DRIVER."),
            @ApiResponse(responseCode = "409", description = "Já existe uma solicitação ativa (PENDING) ou a CNH já está em uso.")
    })
    @PostMapping
    @PreAuthorize("hasRole('PASSENGER')")
    public DriverApplicationSummaryDTO createDriverRequest(
            @AuthenticationPrincipal User author,
            @Valid @RequestBody DriverApplicationRequestDTO driverRequest
    ) {
        var user = userService.findById(driverRequest.requesterId());

        log.info("O usuário {} iniciou a requisição de motorista para o usuário {}", author.getId(), user.getId());
        return driverApplicationService.createDriverApplication(author, user, driverRequest);
    }

    @Operation(
            summary = "Aprova uma solicitação (Apenas ADMIN)",
            description = """
                    Aprova a solicitação pendente e inicia o fluxo assíncrono de upgrade de conta.
                    
                    **Regras de Negócio (RN):**
                    * Apenas aplicações em status PENDING podem ser aprovadas.
                    * A aplicação passa a ter `applicationStatus = APPROVED` e `reviewedBy` preenchido.
                    
                    **Efeitos Colaterais (Assíncronos):**
                    * O usuário é criado como Motorista, e tem sua permissão alterada para DRIVER.
                    * **Expurgo:** O número de segurança da CNH é **expurgado** na entidade - Seguindo o princípio da minimização de dados, assim que essa informação já não é mais relevante para o contexto do sistema, ela é automáticamente removida para evitar exposição.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitação aprovada. Evento publicado para criação do Driver."),
            @ApiResponse(responseCode = "401", description = "Não autenticado."),
            @ApiResponse(responseCode = "403", description = "Acesso negado. Requer Role ADMIN."),
            @ApiResponse(responseCode = "404", description = "Nenhuma solicitação encontrada para o usuário informado."),
            @ApiResponse(responseCode = "409", description = "A solicitação não está mais em estado PENDING (já aprovada, rejeitada ou deletada).")
    })
    @PatchMapping("/{userId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public DriverApplicationSummaryDTO approveDriverRequest(
            @AuthenticationPrincipal User author,
            @PathVariable String userId
    ) {
        return driverApplicationService.approveDriverApplication(author, userId);
    }

    @Operation(
            summary = "Rejeita uma solicitação (Apenas ADMIN)",
            description = """
                    Rejeita a solicitação pendente, com registro obrigatório do motivo.
                    
                    **Regras de Negócio (RN):**
                    * Apenas aplicações em status PENDING podem ser rejeitadas.
                    * A aplicação passa a ter `applicationStatus = DENIED`, `reviewedBy` e `rejectionReason` preenchidos.
                    * O passageiro pode criar uma nova solicitação futuramente (não há lock).
                    * **Expurgo:** O número de segurança da CNH é **expurgado** na entidade - Seguindo o princípio da minimização de dados, assim que essa informação já não é mais relevante para o contexto do sistema, ela é automáticamente removida para evitar exposição.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitação rejeitada. Motivo registrado e código de segurança expurgado."),
            @ApiResponse(responseCode = "400", description = "Corpo da requisição inválido (motivo vazio)."),
            @ApiResponse(responseCode = "401", description = "Não autenticado."),
            @ApiResponse(responseCode = "403", description = "Acesso negado. Requer Role ADMIN."),
            @ApiResponse(responseCode = "404", description = "Nenhuma solicitação encontrada para o usuário informado."),
            @ApiResponse(responseCode = "409", description = "A solicitação não está mais em estado PENDING.")
    })
    @PatchMapping("/{userId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public DriverApplicationSummaryDTO rejectDriverRequest(
            @AuthenticationPrincipal User author,
            @PathVariable String userId,
            @Valid @RequestBody DriverApplicationRejectionDTO dto
    ) {
        return driverApplicationService.rejectDriverApplication(author, userId, dto);
    }

    @Operation(
            summary = "Exclui uma solicitação",
            description = """
                    Permite que o próprio passageiro remova a sua solicitação (Soft Delete).
                    
                    **Regras de Negócio (RN):**
                    * **Ownership:** Apenas o dono da solicitação pode deletá-la (validado no `DriverApplicationValidator.checkOwnership`).
                    * **Soft Delete:** O registro não é apagado do banco. O `status` da entidade muda para `DELETED`.
                    * **Expurgo:** O número de segurança da CNH é **expurgado** na entidade, mesmo que ela fique invisível - Seguindo o princípio da minimização de dados, assim que essa informação já não é mais relevante para o contexto do sistema, ela é automáticamente removida para evitar exposição.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Solicitação excluída com sucesso. Nenhum conteúdo retornado."),
            @ApiResponse(responseCode = "401", description = "Não autenticado."),
            @ApiResponse(responseCode = "403", description = "Tentativa de excluir solicitação de terceiros."),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada pelo ID informado.")
    })
    @DeleteMapping("/{requestId}")
    @PreAuthorize("hasRole('PASSENGER')")
    public void deleteDriverRequest(@AuthenticationPrincipal User author, @PathVariable String requestId) {
        driverApplicationService.delete(requestId, author);
    }

    @Operation(
            summary = "Lista todas as solicitações (Apenas ADMIN)",
            description = """
                    Lista paginada de solicitações para análise do administrador.
                    
                    **Regras de Negócio & Filtros:**
                    * **statuses:** Permite filtrar por um ou mais status (PENDING, APPROVED, DENIED).
                    * **search:** Busca textual por nome ou e-mail do solicitante.
                    * **cnh:** Busca exata por CNH. O termo é **criptografado** com a chave determinística antes de ir para a query, permitindo o `WHERE cnh = ?` no banco.
                    
                    **Visão de Dados (LGPD):**
                    * Retorna apenas dados resumidos do solicitante (sem CPF, sem data de nascimento, sem CNH).
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada retornada."),
            @ApiResponse(responseCode = "401", description = "Não autenticado."),
            @ApiResponse(responseCode = "403", description = "Acesso negado. Requer Role ADMIN.")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<DriverApplicationSummaryDTO> getApplications(
            @RequestParam(required = false) List<DriverApplicationStatus> statuses,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String cnh,
            @PageableDefault(sort = "createdAt", direction = Direction.DESC) Pageable pageable
    ) {
        return driverApplicationService.findBy(statuses, search, cnh, pageable);
    }

    @Operation(
            summary = "Busca os detalhes de uma solicitação (Apenas ADMIN)",
            description = """
                    Retorna os dados completos de uma solicitação para que o ADMIN possa analisar.
                    
                    **Visão de Dados (LGPD):**
                    * Inclui CPF, e-mail, data de nascimento e CNH descriptografada pois o endpoint é específico para ADMINs.
                    * **Não inclui** o código de segurança da CNH (esse tem endpoint dedicado).
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalhes da solicitação retornados."),
            @ApiResponse(responseCode = "401", description = "Não autenticado."),
            @ApiResponse(responseCode = "403", description = "Acesso negado. Requer Role ADMIN."),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada.")
    })
    @GetMapping("/{applicationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public DriverApplicationSummaryDTO getApplication(
            @AuthenticationPrincipal User user,
            @PathVariable String applicationId
    ) {
        return driverApplicationService.findById(applicationId, user);
    }

    @Operation(
            summary = "Revela o código de segurança da CNH (Apenas ADMIN)",
            description = """
                    Descriptografa e retorna o código de segurança da CNH (CVV) sob demanda.
                    
                    **Por que este endpoint existe separado?**
                    O código de segurança é o dado mais sensível do sistema. Por isso:
                    * **Não está no `findById`**: não trafega em toda requisição de detalhe.
                    * **Endpoint dedicado**: acionado apenas quando o ADMIN clica no "olho" da UI.
                    * **Criptografia Randomizada**: Impossível buscar por igualdade, apenas descriptografar sob demanda.
                    
                    **Segurança e Auditoria:**
                    * **Audit Log:** cada acesso é registrado na tabela `audit_logs` com: `action`, `targetId`, `authorId` e **IP do ADMIN** (extraído via `X-Forwarded-For`).
                    * **Expurgo (Data Minimization):** se a solicitação já foi processada (APPROVED/DENIED), o código é `null` e o endpoint retorna 404.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código de segurança descriptografado e retornado. Acesso registrado em auditoria."),
            @ApiResponse(responseCode = "401", description = "Não autenticado."),
            @ApiResponse(responseCode = "403", description = "Acesso negado. Requer Role ADMIN."),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada ou código de segurança já expurgado.")
    })
    @GetMapping("/{applicationId}/security-code")
    @PreAuthorize("hasRole('ADMIN')")
    public CnhSecurityNumberDTO getSecurityCode(
            @AuthenticationPrincipal User adminUser,
            @PathVariable String applicationId,
            HttpServletRequest request
    ) {
        String ip = HttpRequestUtils.getClientIp(request);

        auditLogService.logAction(
                "VIEW_CNH_SECURITY_CODE",
                applicationId,
                "APPLICATION",
                adminUser.getId(),
                ip
        );

        return driverApplicationService.findByDriverApplication(applicationId);
    }
}