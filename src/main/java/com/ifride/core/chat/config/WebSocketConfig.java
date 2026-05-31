package com.ifride.core.chat.config;

import com.ifride.core.auth.repository.UserRepository;
import com.ifride.core.auth.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Broker simples em memória para /topic (broadcast) e /queue (privado)
        registry.enableSimpleBroker("/topic", "/queue");
        // Prefixo para métodos @MessageMapping no controller
        registry.setApplicationDestinationPrefixes("/app");
        // Prefixo para mensagens privadas (/user/queue/messages)
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(new JwtHandshakeInterceptor(jwtService, userRepository))
                .setAllowedOriginPatterns("*");
    }

    /**
     * Transfere a autenticação do handshake para o Principal do STOMP.
     * Chamado no CONNECT frame, antes de qualquer @MessageMapping.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                var accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    var sessionAttrs = accessor.getSessionAttributes();
                    if (sessionAttrs != null) {
                        var auth = (UsernamePasswordAuthenticationToken) sessionAttrs.get("auth");
                        if (auth != null) {
                            accessor.setUser(auth);
                        }
                    }
                }
                return message;
            }
        });
    }
}
