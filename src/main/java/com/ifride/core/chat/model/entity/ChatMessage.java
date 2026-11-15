package com.ifride.core.chat.model.entity;

import com.ifride.core.chat.model.enums.MessageStatus;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String rideId;

    private String senderId;
    private String senderName;

    @Column(nullable = false)
    private String recipientId;

    private String content;

    @Enumerated(EnumType.STRING)
    private MessageStatus messageStatus = MessageStatus.SENT;

    private Instant createdAt = Instant.now();
}
