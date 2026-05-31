package com.ifride.core.chat.model.entity;

import com.ifride.core.chat.model.enums.MessageStatus;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "chat_messages")
public class ChatMessage {

    @Id
    private String id;

    @Indexed
    private String rideId;

    private String senderId;
    private String senderName;

    private String recipientId;

    private String content;

    private MessageStatus messageStatus = MessageStatus.SENT;

    private LocalDateTime createdAt = LocalDateTime.now();
}
