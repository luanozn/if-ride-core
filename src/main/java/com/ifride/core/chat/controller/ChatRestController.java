package com.ifride.core.chat.controller;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.chat.model.dto.ChatMessageDTO;
import com.ifride.core.chat.model.dto.ConversationDTO;
import com.ifride.core.chat.service.ChatService;
import com.ifride.core.chat.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat e Mensageria", description = "Endpoints de histórico de chat e contrato do WebSocket (STOMP).")
@SecurityRequirement(name = "bearerAuth")
public class ChatRestController {

    private final ChatService chatService;
    private final ConversationService conversationService;

    @Operation(
            summary = "Lista as conversas do usuário logado",
            description = "Retorna todas as conversas (salas de chat) onde o usuário é o motorista ou o passageiro, ordenadas pela última mensagem."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de conversas retornada com sucesso."),
            @ApiResponse(responseCode = "401", description = "Não autenticado.")
    })
    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationDTO>> getConversations(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(conversationService.getConversationsForUser(currentUser.getId()));
    }

    @Operation(
            summary = "Lista o histórico de mensagens de uma carona",
            description = """
                    Retorna as mensagens trocadas entre o usuário logado e o outro participante da carona.
                    
                    **Regras de Negócio (RN):**
                    * O usuário só recebe mensagens onde ele é o remetente ou o destinatário.
                    * As mensagens são descriptografadas (AES-GCM) antes de serem retornadas para o Frontend.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico retornado."),
            @ApiResponse(responseCode = "403", description = "Usuário não faz parte desta carona."),
            @ApiResponse(responseCode = "404", description = "Carona ou conversa não encontrada.")
    })
    @GetMapping("/messages/{rideId}")
    public ResponseEntity<List<ChatMessageDTO>> getMessages(@AuthenticationPrincipal User currentUser, @PathVariable String rideId) {
        return ResponseEntity.ok(chatService.getMessagesByRide(currentUser, rideId));
    }

    @Operation(
            summary = "Conta as mensagens não lidas de uma carona",
            description = "Retorna a quantidade de mensagens que o usuário recebeu e ainda não marcou como lidas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contagem retornada com sucesso."),
            @ApiResponse(responseCode = "401", description = "Não autenticado.")
    })
    @GetMapping("/rides/{rideId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @PathVariable String rideId,
            @AuthenticationPrincipal User currentUser) {
        long count = chatService.getUnreadCount(rideId, currentUser);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @Operation(
            summary = "Marca uma mensagem como lida",
            description = "Altera o status da mensagem para `READ`. Apenas o destinatário original da mensagem pode realizar essa ação."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mensagem marcada como lida."),
            @ApiResponse(responseCode = "404", description = "Mensagem não encontrada ou o usuário não é o destinatário.")
    })
    @PutMapping("/messages/{messageId}/read")
    public ResponseEntity<ChatMessageDTO> markAsRead(
            @PathVariable String messageId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(chatService.markAsRead(messageId, currentUser));
    }
}