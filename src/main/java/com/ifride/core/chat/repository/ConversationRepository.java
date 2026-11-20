package com.ifride.core.chat.repository;

import com.ifride.core.chat.model.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, String> {
    @Query("SELECT c FROM Conversation c WHERE c.driverId = :userId OR c.passengerId = :userId")
    List<Conversation> findByParticipant(@Param("userId") String userId);

    @Query("""
        SELECT c FROM Conversation c 
        WHERE c.rideId = :rideId 
        AND ((c.driverId = :userA AND c.passengerId = :userB) 
          OR (c.driverId = :userB AND c.passengerId = :userA))
    """)
    Optional<Conversation> findByRideAndPair(@Param("rideId") String rideId, @Param("userA") String userA, @Param("userB") String userB);

    List<Conversation> findByRideId(String rideId);
}
