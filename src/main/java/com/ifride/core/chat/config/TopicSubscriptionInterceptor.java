package com.ifride.core.chat.config;

import com.ifride.core.auth.model.entity.User;
import com.ifride.core.chat.utils.ChatValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TopicSubscriptionInterceptor implements ChannelInterceptor {

    private final ChatValidator chatValidator;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();

            if (destination != null && destination.startsWith("/topic/rides/")) {
                String rideId = extractRideId(destination);

                var principal = accessor.getUser();
                if (!(principal instanceof Authentication auth) || !(auth.getPrincipal() instanceof User user)) {
                    throw new IllegalArgumentException("Usuário não autenticado ou token inválido.");
                }

                String userId = user.getId();

                try {
                    chatValidator.validateUserInRide(rideId, userId);
                    log.info("Usuário {} autorizado a assinar o tópico da carona {}", userId, rideId);
                } catch (Exception e) {
                    log.warn("Tentativa de acesso não autorizado ao tópico da carona {}. Usuário: {}", rideId, userId);
                    throw new IllegalArgumentException("Acesso negado ao chat desta carona.");
                }
            }
        }
        return message;
    }

    private String extractRideId(String destination) {
        String[] parts = destination.split("/");
        if (parts.length >= 4) {
            return parts[3];
        }
        throw new IllegalArgumentException("Formato de destino STOMP inválido: " + destination);
    }
}