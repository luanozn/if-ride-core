package com.ifride.core.chat.service;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.repository.UserRepository;
import com.ifride.core.chat.model.dto.ChatMessageDTO;
import com.ifride.core.chat.model.dto.SendMessageRequest;
import com.ifride.core.chat.model.entity.ChatMessage;
import com.ifride.core.chat.model.enums.MessageStatus;
import com.ifride.core.chat.repository.ChatMessageRepository;
import com.ifride.core.shared.exceptions.api.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ConversationService conversationService;

    public ChatMessageDTO sendMessage(User sender, SendMessageRequest request) {
        var recipient = userRepository.findById(request.recipientId())
                .orElseThrow(() -> new NotFoundException("Destinatário não encontrado."));

        var message = new ChatMessage();
        message.setRideId(request.rideId());
        message.setSenderId(sender.getId());
        message.setSenderName(sender.getName());
        message.setRecipientId(recipient.getId());
        message.setContent(request.content());
        message.setMessageStatus(MessageStatus.SENT);

        var saved = chatMessageRepository.save(message);
        var dto = toDTO(saved);

        messagingTemplate.convertAndSend(
                "/topic/rides/" + request.rideId() + "/messages", dto);

        return dto;
    }

    public List<ChatMessageDTO> getMessagesByRide(String rideId) {
        return chatMessageRepository
                .findByRideIdOrderByCreatedAtAsc(rideId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public long getUnreadCount(String rideId, String userId) {
        return chatMessageRepository
                .countByRideIdAndRecipientIdAndMessageStatusNot(rideId, userId, MessageStatus.READ);
    }

    public ChatMessageDTO markAsRead(String messageId, User currentUser) {
        var message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Mensagem não encontrada."));

        if (!message.getRecipientId().equals(currentUser.getId())) {
            throw new NotFoundException("Mensagem não encontrada.");
        }

        message.setMessageStatus(MessageStatus.READ);
        return toDTO(chatMessageRepository.save(message));
    }

    private ChatMessageDTO toDTO(ChatMessage msg) {
        return new ChatMessageDTO(
                msg.getId(),
                msg.getRideId(),
                msg.getSenderId(),
                msg.getSenderName(),
                msg.getRecipientId(),
                msg.getContent(),
                msg.getMessageStatus().name(),
                msg.getCreatedAt()
        );
    }
}
