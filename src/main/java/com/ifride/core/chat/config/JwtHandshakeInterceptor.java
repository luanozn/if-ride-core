package com.ifride.core.chat.config;

import com.ifride.core.auth.repository.UserRepository;
import com.ifride.core.auth.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            log.warn("WebSocket handshake rejected: not a servlet request");
            return false;
        }

        String token = extractToken(servletRequest);
        if (token == null) {
            log.warn("WebSocket handshake rejected: no token provided");
            return false;
        }

        try {
            String email = jwtService.validateToken(token);
            var user = userRepository.findByEmail(email);
            if (user == null) {
                log.warn("WebSocket handshake rejected: user not found for email {}", email);
                return false;
            }
            var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            attributes.put("auth", auth);
            return true;
        } catch (Exception e) {
            log.warn("WebSocket handshake rejected: invalid token — {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    private String extractToken(ServletServerHttpRequest request) {
        // Query param: ws://host/ws?token=... (stomp_dart_client sends aqui)
        String token = request.getServletRequest().getParameter("token");
        if (token != null && !token.isBlank()) return token;

        // Fallback: Authorization header
        String authHeader = request.getServletRequest().getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
