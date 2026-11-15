package com.ifride.core.chat.service;

import com.ifride.core.chat.model.dto.ConversationDTO;
import com.ifride.core.chat.model.entity.Conversation;
import com.ifride.core.chat.repository.ConversationRepository;
import com.ifride.core.shared.exceptions.api.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@Log4j2
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ChatAndConversationMapper mapper;

    public void createIfAbsent(
            String rideId,
            String driverId, String driverName,
            String passengerId, String passengerName) {

        var existing = conversationRepository.findByRideAndPair(rideId, driverId, passengerId);

        if (existing.isPresent()) {
            return;
        }

        try {
            Conversation conv = mapper.from(rideId, driverId, driverName, passengerId, passengerName);
            conversationRepository.saveAndFlush(conv);
        } catch (DataIntegrityViolationException e) {
            log.info("Ocorreu um erro de concorrência ao tentar criar a conversa.");
        }
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

    public List<Conversation> getConversationsByRide(String rideId) {
        return conversationRepository.findByRideId(rideId);
    }

    public Conversation save(Conversation conversation) {
        return conversationRepository.save(conversation);
    }

    public Conversation getConversation(String rideId, String sender, String recipient) {
        return conversationRepository.findByRideAndPair(rideId, sender, recipient).orElseThrow(() -> new NotFoundException("A conversa não foi encontrada!"));
    }
}
