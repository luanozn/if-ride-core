package com.ifride.core.chat.repository;

import com.ifride.core.chat.model.entity.ChatMessage;
import com.ifride.core.chat.model.enums.MessageStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    List<ChatMessage> findByRideIdOrderByCreatedAtAsc(String rideId);

    long countByRideIdAndRecipientIdAndMessageStatusNot(
            String rideId, String recipientId, MessageStatus messageStatus);
}
