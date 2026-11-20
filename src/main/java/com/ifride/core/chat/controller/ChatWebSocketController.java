package com.ifride.core.chat.controller;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.chat.model.dto.SendMessageRequest;
import com.ifride.core.chat.service.ChatService;
import com.ifride.core.shared.exceptions.api.ForbiddenException;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.security.Principal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
@Validated
public class ChatWebSocketController {

    private final ChatService chatService;

    @Hidden
    @MessageMapping("/chat.send")
    public void sendMessage(@Valid @Payload SendMessageRequest request, Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth && auth.getPrincipal() instanceof User sender) {
            chatService.sendMessage(sender, request);
        } else {
            throw new ForbiddenException("Usuário não autenticado ou sessão inválida.");
        }
    }

    @Operation(
            summary = "Enviar mensagem (Documentação WebSocket)",
            description = """
                    **ATENÇÃO: ESTE ENDPOINT REST NÃO EXISTE NA PRÁTICA.**
                    Ele serve apenas para documentar o contrato do WebSocket no Swagger.
                    
                    Para enviar uma mensagem de verdade, o Frontend Flutter deve usar o protocolo STOMP:
                    * **URL de Conexão:** `wss://sua-url/ws?token=SEU_JWT`
                    * **Subscribe (Receber):** `/user/queue/messages`
                    * **Publish (Enviar):** `/app/chat.send`
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Apenas simulação de documentação."),
            @ApiResponse(responseCode = "403", description = "Bloqueado se a conversa estiver encerrada ou o usuário não pertencer à carona.")
    })
    @PostMapping("/ws/send-docs")
    @ResponseBody
    public void sendMessageDocumentation(@RequestBody SendMessageRequest request) {
        // Método vazio, serve apenas para gerar o modelo de dados no Swagger.
    }
}