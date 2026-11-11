package com.ifride.core.chat.service;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.auth.repository.UserRepository;
import com.ifride.core.chat.model.dto.ChatMessageDTO;
import com.ifride.core.chat.model.dto.SendMessageRequest;
import com.ifride.core.chat.model.enums.MessageStatus;
import com.ifride.core.chat.repository.ChatMessageRepository;
import com.ifride.core.chat.utils.ChatValidator;
import com.ifride.core.shared.exceptions.api.NotFoundException;
import com.ifride.core.shared.services.RandomizedEncryptorService;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatValidator validator;
    private final ConversationService conversationService;
    private final UserRepository userRepository;
    private final ChatAndConversationMapper mapper;

    public ChatMessageDTO sendMessage(User sender, SendMessageRequest request) {
        validator.validateUserInRide(request.rideId(), sender.getId());


        // ---------------------------------------------------------
        // CASO 1: MENSAGEM DE GRUPO (recipientId nulo)
        // ---------------------------------------------------------
        if (StringUtils.isEmpty(request.recipientId())) {
            var message = mapper.fromRequest(sender, request);
            var saved = chatMessageRepository.save(message);
            var dto = mapper.toDTO(saved);

            messagingTemplate.convertAndSend("/topic/rides/" + request.rideId() + "/messages", dto);

            return dto;
        }

        // ---------------------------------------------------------
        // CASO 2: MENSAGEM PRIVADA (1-a-1)
        // ---------------------------------------------------------
        else {
            var conversation = conversationService.getConversation(request.rideId(), sender.getId(), request.recipientId());
            validator.validateConversation(conversation);
            validator.validateUserInRide(request.rideId(), request.recipientId());
            validator.validatePair(request.rideId(), sender.getId(), request.recipientId());

            var recipient = userRepository.findById(request.recipientId())
                    .orElseThrow(() -> new NotFoundException("Destinatário não encontrado."));

            var message = mapper.fromRequest(sender, request);
            var saved = chatMessageRepository.save(message);

            conversationService.updateLastMessage(
                    request.rideId(),
                    sender.getId(),
                    recipient.getId(),
                    request.content()
            );

            var dto = mapper.toDTO(saved);

            messagingTemplate.convertAndSendToUser(recipient.getEmail(), "/queue/messages", dto);
            messagingTemplate.convertAndSendToUser(sender.getEmail(), "/queue/messages", dto);

            return dto;
        }
    }

    public List<ChatMessageDTO> getMessagesByRide(User currentUser, String rideId) {
        validator.validateUserInRide(rideId, currentUser.getId());

        return chatMessageRepository
                .findVisibleMessagesByRideId(rideId, currentUser.getId())
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    public long getUnreadCount(String rideId, User user) {
        validator.validateUserInRide(rideId, user.getId());

        return chatMessageRepository
                .countByRideIdAndRecipientIdAndMessageStatusNot(rideId, user.getId(), MessageStatus.READ);
    }

    public ChatMessageDTO markAsRead(String messageId, User currentUser) {
        var message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Mensagem não encontrada."));

        if (message.getRecipientId() == null || !message.getRecipientId().equals(currentUser.getId())) {
            throw new NotFoundException("Mensagem não encontrada.");
        }

        message.setMessageStatus(MessageStatus.READ);
        return mapper.toDTO(chatMessageRepository.save(message));
    }

}
