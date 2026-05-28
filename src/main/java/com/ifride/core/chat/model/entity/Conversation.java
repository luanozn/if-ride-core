package com.ifride.core.chat.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "conversations")
public class Conversation {

    @Id
    private String id;

    @Indexed
    private String rideId;

    private String driverId;
    private String driverName;

    private String passengerId;
    private String passengerName;

    private String lastMessage;
    private LocalDateTime lastMessageAt;

    private LocalDateTime createdAt = LocalDateTime.now();
}
