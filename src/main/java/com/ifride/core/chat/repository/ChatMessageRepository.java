package com.ifride.core.chat.repository;

import com.ifride.core.chat.model.entity.ChatMessage;
import com.ifride.core.chat.model.enums.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    @Query("""
    SELECT m FROM ChatMessage m 
    WHERE m.rideId = :rideId 
    AND (m.recipientId IS NULL OR m.senderId = :userId OR m.recipientId = :userId) 
    ORDER BY m.createdAt ASC
   """)
    List<ChatMessage> findVisibleMessagesByRideId(@Param("rideId") String rideId, @Param("userId") String userId);

    long countByRideIdAndRecipientIdAndMessageStatusNot(
            String rideId, String recipientId, MessageStatus messageStatus);
}
