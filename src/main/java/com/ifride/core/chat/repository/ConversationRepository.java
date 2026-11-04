package com.ifride.core.chat.repository;

import com.ifride.core.chat.model.entity.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends MongoRepository<Conversation, String> {

    @Query("""
    {
      'rideId': ?0,
      '$or': [
        { 'driverId': ?1, 'passengerId': ?2 },
        { 'driverId': ?2, 'passengerId': ?1 }
      ]
    }
""")
    Optional<Conversation> findByRideAndPair(String rideId, String userA, String userB);

    @Query("{ '$or': [ { 'driverId': ?0 }, { 'passengerId': ?0 } ] }")
    List<Conversation> findByParticipant(String userId);
}
