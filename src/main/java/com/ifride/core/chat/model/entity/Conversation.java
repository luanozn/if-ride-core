package com.ifride.core.chat.model.entity;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.Data;


@Data
@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String rideId;

    private String driverId;
    private String driverName;

    private String passengerId;
    private String passengerName;

    private String lastMessage;
    private Instant lastMessageAt;

    private Instant createdAt = Instant.now();
    private boolean isBlocked = false;
}
