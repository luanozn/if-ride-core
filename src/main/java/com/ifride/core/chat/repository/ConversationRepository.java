package com.ifride.core.chat.repository;

import com.ifride.core.chat.model.entity.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends MongoRepository<Conversation, String> {

    Optional<Conversation> findByRideIdAndDriverIdAndPassengerId(
            String rideId, String driverId, String passengerId);

    @Query("{ '$or': [ { 'driverId': ?0 }, { 'passengerId': ?0 } ] }")
    List<Conversation> findByParticipant(String userId);
}
