package com.ifride.core.chat.controller;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.chat.model.dto.ChatMessageDTO;
import com.ifride.core.chat.model.dto.ConversationDTO;
import com.ifride.core.chat.service.ChatService;
import com.ifride.core.chat.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatService chatService;
    private final ConversationService conversationService;

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationDTO>> getConversations(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(conversationService.getConversationsForUser(currentUser.getId()));
    }

    @GetMapping("/messages/{rideId}")
    public ResponseEntity<List<ChatMessageDTO>> getMessages(@PathVariable String rideId) {
        return ResponseEntity.ok(chatService.getMessagesByRide(rideId));
    }

    @GetMapping("/rides/{rideId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @PathVariable String rideId,
            @AuthenticationPrincipal User currentUser) {
        long count = chatService.getUnreadCount(rideId, currentUser.getId());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PutMapping("/messages/{messageId}/read")
    public ResponseEntity<ChatMessageDTO> markAsRead(
            @PathVariable String messageId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(chatService.markAsRead(messageId, currentUser));
    }
}