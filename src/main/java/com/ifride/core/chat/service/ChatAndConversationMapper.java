package com.ifride.core.chat.service;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.repository.UserRepository;
import com.ifride.core.chat.model.dto.ChatMessageDTO;
import com.ifride.core.chat.model.dto.ConversationDTO;
import com.ifride.core.chat.model.dto.SendMessageRequest;
import com.ifride.core.chat.model.entity.ChatMessage;
import com.ifride.core.chat.model.entity.Conversation;
import com.ifride.core.chat.model.enums.MessageStatus;
import com.ifride.core.shared.exceptions.api.NotFoundException;
import com.ifride.core.shared.services.RandomizedEncryptorService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatAndConversationMapper {

    private final RandomizedEncryptorService crypto;

    public ChatMessage fromRequest(User sender, SendMessageRequest request) {
        var message = new ChatMessage();
        message.setRideId(request.rideId());
        message.setSenderId(sender.getId());
        message.setSenderName(sender.getName());
        message.setRecipientId(request.recipientId());
        message.setContent(crypto.encrypt(request.content()));
        message.setMessageStatus(MessageStatus.SENT);

        return message;
    }

    public ChatMessageDTO toDTO(ChatMessage msg) {
        String content;
        try {
            content = crypto.decrypt(msg.getContent());
        } catch (Exception e) {
            content = "[Mensagem corrompida ou indisponível]";
        }

        return new ChatMessageDTO(
                msg.getId(),
                msg.getRideId(),
                msg.getSenderId(),
                msg.getSenderName(),
                msg.getRecipientId(),
                content,
                msg.getMessageStatus().name(),
                msg.getCreatedAt()
        );
    }

    public Conversation updatedContent(Conversation conv, String content) {
        conv.setLastMessage(crypto.encrypt(content));
        conv.setLastMessageAt(Instant.now());
        return conv;
    }

    public Conversation from(String rideId, String driverId, String driverName, String passengerId, String passengerName) {
        var conv = new Conversation();
        conv.setRideId(rideId);
        conv.setDriverId(driverId);
        conv.setDriverName(driverName);
        conv.setPassengerId(passengerId);
        conv.setPassengerName(passengerName);
        return conv;
    }

    public ConversationDTO from(Conversation c) {
        String lastMessage = c.getLastMessage() != null
                ? crypto.decrypt(c.getLastMessage())
                : null;

        return new ConversationDTO(
                c.getId(),
                c.getRideId(),
                c.getDriverId(),
                c.getDriverName(),
                c.getPassengerId(),
                c.getPassengerName(),
                lastMessage,
                c.getLastMessageAt(),
                c.getCreatedAt()
        );
    }
}
