package com.ifride.core.chat.service;

import com.ifride.core.chat.model.dto.ConversationDTO;
import com.ifride.core.chat.model.entity.Conversation;
import com.ifride.core.shared.services.RandomizedEncryptorService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConversationMapper {

    private final RandomizedEncryptorService crypto;

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
