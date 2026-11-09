package com.ifride.core.chat.service;

import com.ifride.core.chat.model.dto.ConversationDTO;
import com.ifride.core.chat.model.entity.Conversation;
import com.ifride.core.chat.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ChatAndConversationMapper mapper;

    public ConversationDTO createIfAbsent(
            String rideId,
            String driverId, String driverName,
            String passengerId, String passengerName) {

        return conversationRepository
                .findByRideAndPair(rideId, driverId, passengerId)
                .map(mapper::from)
                .orElseGet(() -> {
                    Conversation conv = mapper.from(rideId, driverId, driverName, passengerId, passengerName);
                    return mapper.from(conversationRepository.save(conv));
                });
    }

    public void updateLastMessage(String rideId, String sender, String recipient, String content) {
        conversationRepository
                .findByRideAndPair(rideId, sender, recipient)
                .ifPresent(conv -> conversationRepository.save(mapper.updatedContent(conv, content)));
    }

    public List<ConversationDTO> getConversationsForUser(String userId) {
        return conversationRepository.findByParticipant(userId)
                .stream()
                .sorted(Comparator.comparing(
                        Conversation::getLastMessageAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(mapper::from)
                .toList();
    }
}
