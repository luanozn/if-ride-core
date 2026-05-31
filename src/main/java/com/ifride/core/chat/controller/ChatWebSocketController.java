package com.ifride.core.chat.controller;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.chat.model.dto.SendMessageRequest;
import com.ifride.core.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendMessageRequest request, Principal principal) {
        var auth = (UsernamePasswordAuthenticationToken) principal;
        var sender = (User) auth.getPrincipal();
        chatService.sendMessage(sender, request);
    }
}
