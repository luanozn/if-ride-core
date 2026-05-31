package com.ifride.core.chat.service;

import com.ifride.core.chat.model.dto.ConversationDTO;
import com.ifride.core.chat.model.entity.Conversation;
import com.ifride.core.chat.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public Conversation createIfAbsent(
            String rideId,
            String driverId, String driverName,
            String passengerId, String passengerName) {

        return conversationRepository
                .findByRideIdAndDriverIdAndPassengerId(rideId, driverId, passengerId)
                .orElseGet(() -> {
                    var conv = new Conversation();
                    conv.setRideId(rideId);
                    conv.setDriverId(driverId);
                    conv.setDriverName(driverName);
                    conv.setPassengerId(passengerId);
                    conv.setPassengerName(passengerName);
                    return conversationRepository.save(conv);
                });
    }

    public void updateLastMessage(String rideId, String driverId, String passengerId, String content) {
        conversationRepository
                .findByRideIdAndDriverIdAndPassengerId(rideId, driverId, passengerId)
                .ifPresent(conv -> {
                    conv.setLastMessage(content);
                    conv.setLastMessageAt(LocalDateTime.now());
                    conversationRepository.save(conv);
                });
    }

    public List<ConversationDTO> getConversationsForUser(String userId) {
        return conversationRepository.findByParticipant(userId)
                .stream()
                .sorted(Comparator.comparing(
                        Conversation::getLastMessageAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(ConversationDTO::fromEntity)
                .toList();
    }
}
